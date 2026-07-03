package com.aztu.support.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectRegistrationRequest(@NotBlank @Size(max = 500) String reason) {
}
