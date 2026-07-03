package com.aztu.support.domain.enums;

/**
 * Every meaningful, notifiable event in the system. Each type maps to an
 * in-dashboard notification and an email template.
 */
public enum NotificationType {
    TICKET_OPENED,
    TICKET_ASSIGNED,
    TICKET_STATUS_CHANGED,
    TICKET_CLOSED,
    COMMENT_ADDED,
    REGISTRATION_PENDING,
    REGISTRATION_APPROVED,
    REGISTRATION_REJECTED,
    ROLE_CHANGED
}
