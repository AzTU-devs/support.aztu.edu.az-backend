package com.aztu.support.dto.user;

import com.aztu.support.domain.enums.RoleName;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(@NotNull RoleName role) {
}
