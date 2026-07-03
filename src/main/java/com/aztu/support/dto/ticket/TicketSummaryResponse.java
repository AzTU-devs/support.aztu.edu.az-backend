package com.aztu.support.dto.ticket;

import com.aztu.support.domain.Ticket;
import com.aztu.support.domain.enums.TicketPriority;
import com.aztu.support.domain.enums.TicketStatus;
import java.time.Instant;

/** Row shape used in ticket lists. */
public record TicketSummaryResponse(
        Long id,
        String ticketNumber,
        String subject,
        TicketPriority priority,
        TicketStatus status,
        String platformName,
        String categoryName,
        String createdByName,
        String assignedToName,
        Instant createdAt,
        Instant updatedAt) {

    public static TicketSummaryResponse from(Ticket t) {
        return new TicketSummaryResponse(
                t.getId(),
                t.getTicketNumber(),
                t.getSubject(),
                t.getPriority(),
                t.getStatus(),
                t.getPlatform() != null ? t.getPlatform().getName() : null,
                t.getCategory() != null ? t.getCategory().getName() : null,
                t.getCreatedBy() != null ? t.getCreatedBy().fullName() : null,
                t.getAssignedTo() != null ? t.getAssignedTo().fullName() : null,
                t.getCreatedAt(),
                t.getUpdatedAt());
    }
}
