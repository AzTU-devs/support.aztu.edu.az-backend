package com.aztu.support.dto.ticket;

/** Assign (or, with a null id, unassign) a ticket to a support agent. */
public record AssignTicketRequest(Long assigneeId) {
}
