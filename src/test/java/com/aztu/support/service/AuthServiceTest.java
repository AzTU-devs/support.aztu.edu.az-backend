package com.aztu.support.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aztu.support.config.AppProperties;
import com.aztu.support.domain.Role;
import com.aztu.support.domain.User;
import com.aztu.support.domain.enums.AccountStatus;
import com.aztu.support.domain.enums.RoleName;
import com.aztu.support.dto.auth.AuthResponse;
import com.aztu.support.dto.auth.LoginRequest;
import com.aztu.support.dto.auth.RegisterRequest;
import com.aztu.support.exception.ApiException;
import com.aztu.support.repository.EmailVerificationTokenRepository;
import com.aztu.support.repository.PasswordResetTokenRepository;
import com.aztu.support.repository.RefreshTokenRepository;
import com.aztu.support.repository.RoleRepository;
import com.aztu.support.repository.UserRepository;
import com.aztu.support.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock TokenGenerator tokenGenerator;
    @Mock NotificationService notificationService;
    @Mock EmailService emailService;

    AppProperties props;
    AuthService authService;

    @BeforeEach
    void setUp() {
        props = new AppProperties();
        props.setAllowedEmailDomain("aztu.edu.az");
        props.setRequireEmailVerification(true);
        props.getJwt().setRefreshTokenExpirationDays(14);
        authService = new AuthService(userRepository, roleRepository, refreshTokenRepository,
                passwordResetTokenRepository, emailVerificationTokenRepository, passwordEncoder,
                jwtService, tokenGenerator, notificationService, emailService, props);
    }

    // ── Email domain restriction ─────────────────────────────────────────────

    @Test
    void register_rejectsNonCorporateDomain() {
        RegisterRequest request = new RegisterRequest("Jane", "Doe", "jane.doe@gmail.com", "password123");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_rejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("jane.doe@aztu.edu.az")).thenReturn(true);
        RegisterRequest request = new RegisterRequest("Jane", "Doe", "Jane.Doe@aztu.edu.az", "password123");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void register_createsPendingUserAndNotifies() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(role(RoleName.USER)));
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenGenerator.generateRawToken()).thenReturn("raw");
        when(tokenGenerator.hash("raw")).thenReturn("hash");

        var response = authService.register(
                new RegisterRequest("Jane", "Doe", "jane.doe@aztu.edu.az", "password123"));

        assertThat(response.status()).isEqualTo(AccountStatus.PENDING_APPROVAL);
        assertThat(response.role()).isEqualTo(RoleName.USER);
        verify(emailService).send(anyString(), anyString(), any());
        verify(notificationService).registrationSubmitted(any(User.class));
    }

    // ── Approval-before-login enforcement ────────────────────────────────────

    @Test
    void login_blocksPendingApproval() {
        User user = user(AccountStatus.PENDING_APPROVAL, true);
        when(userRepository.findByEmailIgnoreCase("jane.doe@aztu.edu.az")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("jane.doe@aztu.edu.az", "password123")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void login_blocksRejectedAndDisabled() {
        for (AccountStatus status : new AccountStatus[]{AccountStatus.REJECTED, AccountStatus.DISABLED}) {
            User user = user(status, true);
            when(userRepository.findByEmailIgnoreCase("jane.doe@aztu.edu.az")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);

            assertThatThrownBy(() -> authService.login(new LoginRequest("jane.doe@aztu.edu.az", "password123")))
                    .isInstanceOf(ApiException.class)
                    .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        }
    }

    @Test
    void login_rejectsWrongPassword() {
        User user = user(AccountStatus.ACTIVE, true);
        when(userRepository.findByEmailIgnoreCase("jane.doe@aztu.edu.az")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("jane.doe@aztu.edu.az", "wrong")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void login_requiresVerifiedEmail() {
        User user = user(AccountStatus.ACTIVE, false);
        when(userRepository.findByEmailIgnoreCase("jane.doe@aztu.edu.az")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("jane.doe@aztu.edu.az", "password123")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void login_succeedsForActiveVerifiedUser() {
        User user = user(AccountStatus.ACTIVE, true);
        when(userRepository.findByEmailIgnoreCase("jane.doe@aztu.edu.az")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(1800L);
        when(tokenGenerator.generateRawToken()).thenReturn("refresh-raw");
        lenient().when(tokenGenerator.hash("refresh-raw")).thenReturn("refresh-hash");

        AuthResponse response = authService.login(new LoginRequest("jane.doe@aztu.edu.az", "password123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-raw");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        verify(refreshTokenRepository).save(any());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Role role(RoleName name) {
        Role role = new Role();
        role.setId(1L);
        role.setName(name);
        return role;
    }

    private User user(AccountStatus status, boolean verified) {
        User user = new User();
        user.setId(10L);
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail("jane.doe@aztu.edu.az");
        user.setPasswordHash("hashed");
        user.setRole(role(RoleName.USER));
        user.setStatus(status);
        user.setEmailVerified(verified);
        return user;
    }
}
