package com.aztu.support.dto.ticket;

import com.aztu.support.domain.Ticket;
import com.aztu.support.domain.enums.TicketPriority;
import com.aztu.support.domain.enums.TicketStatus;
import com.aztu.support.dto.user.UserSummaryResponse;
import java.time.Instant;
import java.util.List;

/** Full ticket view: fields + comment thread + status history + attachments. */
public record TicketDetailResponse(
        Long id,
        String ticketNumber,
        String subject,
        String description,
        TicketPriority priority,
        TicketStatus status,
        Long platformId,
        String platformName,
        Long categoryId,
        String categoryName,
        UserSummaryResponse createdBy,
        UserSummaryResponse assignedTo,
        String resolutionNote,
        Instant createdAt,
        Instant updatedAt,
        Instant closedAt,
        List<CommentResponse> comments,
        List<StatusHistoryResponse> statusHistory,
        List<AttachmentResponse> attachments) {

    public static TicketDetailResponse from(Ticket t,
                                            List<CommentResponse> comments,
                                            List<StatusHistoryResponse> statusHistory,
                                            List<AttachmentResponse> attachments) {
        return new TicketDetailResponse(
                t.getId(),
                t.getTicketNumber(),
                t.getSubject(),
                t.getDescription(),
                t.getPriority(),
                t.getStatus(),
                t.getPlatform() != null ? t.getPlatform().getId() : null,
                t.getPlatform() != null ? t.getPlatform().getName() : null,
                t.getCategory() != null ? t.getCategory().getId() : null,
                t.getCategory() != null ? t.getCategory().getName() : null,
                UserSummaryResponse.from(t.getCreatedBy()),
                UserSummaryResponse.from(t.getAssignedTo()),
                t.getResolutionNote(),
                t.getCreatedAt(),
                t.getUpdatedAt(),
                t.getClosedAt(),
                comments,
                statusHistory,
                attachments);
    }
}
