package com.aztu.support.dto.notification;

import com.aztu.support.domain.Notification;
import com.aztu.support.domain.enums.NotificationType;
import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        String link,
        Long ticketId,
        boolean read,
        Instant createdAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getLink(),
                n.getTicket() != null ? n.getTicket().getId() : null,
                n.isRead(),
                n.getCreatedAt());
    }
}
