package com.aztu.support.dto.ticket;

import com.aztu.support.domain.TicketStatusHistory;
import com.aztu.support.domain.enums.TicketStatus;
import java.time.Instant;

public record StatusHistoryResponse(
        Long id,
        TicketStatus fromStatus,
        TicketStatus toStatus,
        String reason,
        String changedByName,
        Instant createdAt) {

    public static StatusHistoryResponse from(TicketStatusHistory h) {
        return new StatusHistoryResponse(
                h.getId(),
                h.getFromStatus(),
                h.getToStatus(),
                h.getReason(),
                h.getChangedBy().fullName(),
                h.getCreatedAt());
    }
}
