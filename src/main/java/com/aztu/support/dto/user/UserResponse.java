package com.aztu.support.dto.user;

import com.aztu.support.domain.User;
import com.aztu.support.domain.enums.AccountStatus;
import com.aztu.support.domain.enums.RoleName;
import java.time.Instant;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        String email,
        RoleName role,
        AccountStatus status,
        boolean emailVerified,
        String rejectionReason,
        Instant createdAt,
        Instant approvedAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.fullName(),
                user.getEmail(),
                user.roleName(),
                user.getStatus(),
                user.isEmailVerified(),
                user.getRejectionReason(),
                user.getCreatedAt(),
                user.getApprovedAt());
    }
}
