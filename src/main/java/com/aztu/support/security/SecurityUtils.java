package com.aztu.support.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Convenience access to the currently authenticated principal. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AppUserPrincipal currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUserPrincipal principal) {
            return principal;
        }
        return null;
    }

    public static Long currentUserId() {
        AppUserPrincipal principal = currentPrincipal();
        return principal != null ? principal.getId() : null;
    }
}
