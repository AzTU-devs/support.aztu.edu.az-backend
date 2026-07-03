package com.aztu.support.service;

import com.aztu.support.domain.Role;
import com.aztu.support.domain.User;
import com.aztu.support.domain.enums.AccountStatus;
import com.aztu.support.domain.enums.RoleName;
import com.aztu.support.dto.user.UserResponse;
import com.aztu.support.dto.user.UserSummaryResponse;
import com.aztu.support.exception.ApiException;
import com.aztu.support.repository.RefreshTokenRepository;
import com.aztu.support.repository.RoleRepository;
import com.aztu.support.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final List<RoleName> SUPPORT_ROLES =
            List.of(RoleName.SUPPORT_TEAM, RoleName.ADMIN, RoleName.SUPER_ADMIN);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final NotificationService notificationService;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       NotificationService notificationService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.notificationService = notificationService;
    }

    // ── Reads ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return UserResponse.from(loadUser(id));
    }

    @Transactional(readOnly = true)
    public User loadUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("User not found."));
    }

    @Transactional(readOnly = true)
    public Page<User> list(AccountStatus status, RoleName role, Pageable pageable) {
        if (status != null) {
            return userRepository.findByStatus(status, pageable);
        }
        if (role != null) {
            return userRepository.findByRole_Name(role, pageable);
        }
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<User> pendingApprovals(Pageable pageable) {
        return userRepository.findByStatus(AccountStatus.PENDING_APPROVAL, pageable);
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> supportAgents() {
        return userRepository.findByRole_NameInAndStatus(SUPPORT_ROLES, AccountStatus.ACTIVE).stream()
                .map(UserSummaryResponse::from)
                .toList();
    }

    // ── Registration approval (ADMIN / SUPER_ADMIN) ──────────────────────────

    @Transactional
    public UserResponse approve(Long adminId, Long userId, RoleName roleToAssign) {
        User user = loadUser(userId);
        if (user.getStatus() != AccountStatus.PENDING_APPROVAL) {
            throw ApiException.badRequest("This account is not pending approval.");
        }
        if (roleToAssign != null) {
            user.setRole(resolveRole(roleToAssign));
        }
        user.setStatus(AccountStatus.ACTIVE);
        user.setApprovedBy(userRepository.findById(adminId).orElse(null));
        user.setApprovedAt(Instant.now());
        user.setRejectionReason(null);
        userRepository.save(user);
        notificationService.registrationApproved(user);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse reject(Long adminId, Long userId, String reason) {
        User user = loadUser(userId);
        if (user.getStatus() != AccountStatus.PENDING_APPROVAL) {
            throw ApiException.badRequest("This account is not pending approval.");
        }
        user.setStatus(AccountStatus.REJECTED);
        user.setRejectionReason(reason);
        user.setApprovedBy(userRepository.findById(adminId).orElse(null));
        userRepository.save(user);
        notificationService.registrationRejected(user, reason);
        return UserResponse.from(user);
    }

    // ── Role / status management (SUPER_ADMIN) ───────────────────────────────

    @Transactional
    public UserResponse changeRole(Long actorId, Long userId, RoleName newRole) {
        User user = loadUser(userId);
        if (user.getRole().getName() == RoleName.SUPER_ADMIN && newRole != RoleName.SUPER_ADMIN
                && countActiveSuperAdmins() <= 1) {
            throw ApiException.badRequest("Cannot remove the last active SUPER_ADMIN.");
        }
        if (user.getRole().getName() == newRole) {
            return UserResponse.from(user);
        }
        user.setRole(resolveRole(newRole));
        userRepository.save(user);
        notificationService.roleChanged(user, newRole);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateStatus(Long actorId, Long userId, AccountStatus status) {
        User user = loadUser(userId);
        if (actorId.equals(userId) && status != AccountStatus.ACTIVE) {
            throw ApiException.badRequest("You cannot disable your own account.");
        }
        user.setStatus(status);
        userRepository.save(user);
        if (status == AccountStatus.DISABLED || status == AccountStatus.REJECTED) {
            refreshTokenRepository.revokeAllForUser(user.getId());
        }
        return UserResponse.from(user);
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private Role resolveRole(RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> ApiException.badRequest("Unknown role: " + roleName));
    }

    private long countActiveSuperAdmins() {
        return userRepository.findByRole_NameInAndStatus(
                List.of(RoleName.SUPER_ADMIN), AccountStatus.ACTIVE).size();
    }
}
