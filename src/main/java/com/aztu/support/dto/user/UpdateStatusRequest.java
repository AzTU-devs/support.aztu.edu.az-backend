package com.aztu.support.dto.user;

import com.aztu.support.domain.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;

/** Enable / disable an account (SUPER_ADMIN). */
public record UpdateStatusRequest(@NotNull AccountStatus status) {
}
