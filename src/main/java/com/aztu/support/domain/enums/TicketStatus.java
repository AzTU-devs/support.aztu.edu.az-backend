package com.aztu.support.domain.enums;

/**
 * Ticket lifecycle statuses. The enum is intentionally extensible.
 * <p>
 * Any status flagged {@code closing} terminates the ticket and therefore
 * REQUIRES a mandatory resolution/reason note (enforced on the backend).
 */
public enum TicketStatus {
    OPEN(false),
    IN_PROGRESS(false),
    RESOLVED(true),
    CLOSED_WITHOUT_RESOLVE(true),
    REJECTED(true);

    private final boolean closing;

    TicketStatus(boolean closing) {
        this.closing = closing;
    }

    /** @return true if moving to this status closes the ticket and a reason is mandatory. */
    public boolean isClosing() {
        return closing;
    }
}
