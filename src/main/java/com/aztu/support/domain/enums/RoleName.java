package com.aztu.support.domain.enums;

/**
 * The four fixed roles in the system. Stored in the {@code roles} table and
 * exposed to Spring Security as authorities of the form {@code ROLE_<name>}.
 */
public enum RoleName {
    USER,
    SUPPORT_TEAM,
    ADMIN,
    SUPER_ADMIN
}
