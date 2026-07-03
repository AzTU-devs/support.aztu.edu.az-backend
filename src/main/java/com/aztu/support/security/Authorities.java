package com.aztu.support.security;

/**
 * Reusable {@code @PreAuthorize} SpEL expressions. Roles are listed explicitly
 * (rather than relying on the role hierarchy) so authorization is unambiguous
 * and version-independent.
 */
public final class Authorities {

    private Authorities() {
    }

    /** Any signed-in, active user. */
    public static final String AUTHENTICATED = "isAuthenticated()";

    /** Support team and above (can manage ticket status/assignment). */
    public static final String SUPPORT = "hasAnyRole('SUPPORT_TEAM','ADMIN','SUPER_ADMIN')";

    /** Admins and above (approvals + platform/category management). */
    public static final String ADMIN = "hasAnyRole('ADMIN','SUPER_ADMIN')";

    /** Super admins only (role & account-status management). */
    public static final String SUPER_ADMIN = "hasRole('SUPER_ADMIN')";
}
