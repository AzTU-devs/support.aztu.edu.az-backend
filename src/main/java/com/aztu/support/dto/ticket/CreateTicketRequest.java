package com.aztu.support.dto.ticket;

import com.aztu.support.domain.enums.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotNull Long platformId,
        Long categoryId,
        @NotBlank @Size(max = 255) String subject,
        @NotBlank @Size(max = 10000) String description,
        TicketPriority priority) {
}
