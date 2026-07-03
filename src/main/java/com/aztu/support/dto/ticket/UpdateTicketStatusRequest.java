package com.aztu.support.dto.ticket;

import com.aztu.support.domain.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Change a ticket's status. {@code reason} is mandatory when moving to a closing
 * status (RESOLVED, CLOSED_WITHOUT_RESOLVE, REJECTED) — enforced in the service.
 */
public record UpdateTicketStatusRequest(
        @NotNull TicketStatus status,
        @Size(max = 10000) String reason) {
}
