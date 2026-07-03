package com.aztu.support.domain.enums;

/**
 * Lifecycle of a user account. Only {@link #ACTIVE} accounts may sign in.
 */
public enum AccountStatus {
    PENDING_APPROVAL,
    ACTIVE,
    REJECTED,
    DISABLED
}
