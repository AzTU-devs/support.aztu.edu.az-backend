package com.aztu.support.dto.user;

import com.aztu.support.domain.User;
import com.aztu.support.domain.enums.RoleName;

/** Lightweight user reference embedded in tickets, comments, etc. */
public record UserSummaryResponse(Long id, String fullName, String email, RoleName role) {

    public static UserSummaryResponse from(User user) {
        if (user == null) {
            return null;
        }
        return new UserSummaryResponse(user.getId(), user.fullName(), user.getEmail(), user.roleName());
    }
}
