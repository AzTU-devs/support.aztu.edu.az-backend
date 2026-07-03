package com.aztu.support.dto.auth;

import jakarta.validation.constraints.NotBlank;

/** Wraps a single opaque token (used for email verification and logout). */
public record TokenRequest(@NotBlank String token) {
}
