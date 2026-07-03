package com.aztu.support.service;

import com.aztu.support.config.AppProperties;
import com.aztu.support.domain.EmailVerificationToken;
import com.aztu.support.domain.PasswordResetToken;
import com.aztu.support.domain.RefreshToken;
import com.aztu.support.domain.Role;
import com.aztu.support.domain.User;
import com.aztu.support.domain.enums.AccountStatus;
import com.aztu.support.domain.enums.RoleName;
import com.aztu.support.dto.auth.AuthResponse;
import com.aztu.support.dto.auth.ChangePasswordRequest;
import com.aztu.support.dto.auth.LoginRequest;
import com.aztu.support.dto.auth.RegisterRequest;
import com.aztu.support.dto.user.UserResponse;
import com.aztu.support.exception.ApiException;
import com.aztu.support.repository.EmailVerificationTokenRepository;
import com.aztu.support.repository.PasswordResetTokenRepository;
import com.aztu.support.repository.RefreshTokenRepository;
import com.aztu.support.repository.RoleRepository;
import com.aztu.support.repository.UserRepository;
import com.aztu.support.security.JwtService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenGenerator tokenGenerator;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final AppProperties props;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       EmailVerificationTokenRepository emailVerificationTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       TokenGenerator tokenGenerator,
                       NotificationService notificationService,
                       EmailService emailService,
                       AppProperties props) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenGenerator = tokenGenerator;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.props = props;
    }

    // ── Registration ─────────────────────────────────────────────────────────

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        validateEmailDomain(email);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists.");
        }

        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException("USER role missing — check seed data."));

        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(userRole);
        user.setStatus(AccountStatus.PENDING_APPROVAL);
        user.setEmailVerified(false);
        user = userRepository.save(user);

        sendVerificationEmail(user);
        notificationService.registrationSubmitted(user);

        log.info("Registered new user {} (pending approval)", user.getEmail());
        return UserResponse.from(user);
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        EmailVerificationToken token = emailVerificationTokenRepository.findByTokenHash(tokenGenerator.hash(rawToken))
                .orElseThrow(() -> ApiException.badRequest("Invalid or expired verification link."));
        if (!token.isValid()) {
            throw ApiException.badRequest("This verification link has expired or was already used.");
        }
        token.setUsed(true);
        User user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        log.info("Email verified for {}", user.getEmail());
    }

    @Transactional
    public void resendVerificationEmail(String rawEmail) {
        userRepository.findByEmailIgnoreCase(normalize(rawEmail)).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                sendVerificationEmail(user);
            }
        });
    }

    // ── Login / tokens ───────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalize(request.email());
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid email or password.");
        }

        // Approval-before-login and account-state enforcement (backend authoritative).
        switch (user.getStatus()) {
            case PENDING_APPROVAL -> throw ApiException.forbidden(
                    "Your account is pending approval by an administrator. You'll be notified once it's approved.");
            case REJECTED -> throw ApiException.forbidden(
                    "Your registration was rejected. Please contact the IT department.");
            case DISABLED -> throw ApiException.forbidden(
                    "Your account has been disabled. Please contact the IT department.");
            case ACTIVE -> { /* continue */ }
        }

        if (props.isRequireEmailVerification() && !user.isEmailVerified()) {
            throw ApiException.forbidden("Please verify your email address before signing in.");
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenGenerator.hash(rawRefreshToken))
                .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token."));
        if (!stored.isActive()) {
            throw ApiException.unauthorized("Your session has expired. Please sign in again.");
        }
        User user = stored.getUser();
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw ApiException.forbidden("Your account is not active.");
        }
        // Rotation: revoke the presented token and issue a brand-new pair.
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(tokenGenerator.hash(rawRefreshToken)).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    // ── Password flows ───────────────────────────────────────────────────────

    @Transactional
    public void forgotPassword(String rawEmail) {
        // Always behave identically whether or not the account exists (no enumeration).
        userRepository.findByEmailIgnoreCase(normalize(rawEmail)).ifPresent(user -> {
            if (user.getStatus() == AccountStatus.DISABLED || user.getStatus() == AccountStatus.REJECTED) {
                return;
            }
            passwordResetTokenRepository.invalidateAllForUser(user.getId());
            String raw = tokenGenerator.generateRawToken();
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setTokenHash(tokenGenerator.hash(raw));
            token.setExpiresAt(Instant.now().plus(
                    props.getTokens().getPasswordResetExpirationMinutes(), ChronoUnit.MINUTES));
            passwordResetTokenRepository.save(token);

            String url = frontendLink("/reset-password?token=" + raw);
            emailService.send(user.getEmail(), "Reset your AzTU Support password",
                    EmailContent.builder()
                            .heading("Reset your password")
                            .greeting("Hello " + user.getFirstName() + ",")
                            .paragraphs(java.util.List.of(
                                    "We received a request to reset your AzTU Support password.",
                                    "This link expires in "
                                            + props.getTokens().getPasswordResetExpirationMinutes() + " minutes."))
                            .cta("Reset password", url)
                            .footerNote("If you didn't request this, you can safely ignore this email.")
                            .build());
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(tokenGenerator.hash(rawToken))
                .orElseThrow(() -> ApiException.badRequest("Invalid or expired reset link."));
        if (!token.isValid()) {
            throw ApiException.badRequest("This reset link has expired or was already used.");
        }
        token.setUsed(true);
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        // Invalidate all existing sessions.
        refreshTokenRepository.revokeAllForUser(user.getId());
        log.info("Password reset for {}", user.getEmail());
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found."));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("Your current password is incorrect.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllForUser(user.getId());
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefresh = tokenGenerator.generateRawToken();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(tokenGenerator.hash(rawRefresh));
        refreshToken.setExpiresAt(Instant.now().plus(
                props.getJwt().getRefreshTokenExpirationDays(), ChronoUnit.DAYS));
        refreshTokenRepository.save(refreshToken);
        return AuthResponse.of(accessToken, rawRefresh, jwtService.getAccessTokenTtlSeconds(),
                UserResponse.from(user));
    }

    private void sendVerificationEmail(User user) {
        emailVerificationTokenRepository.invalidateAllForUser(user.getId());
        String raw = tokenGenerator.generateRawToken();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(tokenGenerator.hash(raw));
        token.setExpiresAt(Instant.now().plus(
                props.getTokens().getEmailVerificationExpirationHours(), ChronoUnit.HOURS));
        emailVerificationTokenRepository.save(token);

        String url = frontendLink("/verify-email?token=" + raw);
        emailService.send(user.getEmail(), "Verify your AzTU Support email",
                EmailContent.builder()
                        .heading("Verify your email address")
                        .greeting("Hello " + user.getFirstName() + ",")
                        .paragraphs(java.util.List.of(
                                "Thanks for registering with AzTU Support.",
                                "Please confirm your email address. After confirming, an administrator "
                                        + "will review and approve your account before you can sign in."))
                        .cta("Verify email", url)
                        .build());
    }

    private void validateEmailDomain(String email) {
        String domain = props.getAllowedEmailDomain().toLowerCase().trim();
        int at = email.indexOf('@');
        if (at <= 0 || !email.endsWith("@" + domain)) {
            throw ApiException.badRequest(
                    "Registration is restricted to @" + domain + " email addresses.");
        }
    }

    private String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String frontendLink(String path) {
        String base = props.getFrontendUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + path;
    }
}
