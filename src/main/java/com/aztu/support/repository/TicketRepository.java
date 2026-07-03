package com.aztu.support.repository;

import com.aztu.support.domain.Ticket;
import com.aztu.support.domain.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface TicketRepository
        extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    /** Draws the next value from the DB sequence used to build human-friendly ticket numbers. */
    @Query(value = "SELECT nextval('ticket_number_seq')", nativeQuery = true)
    long nextTicketNumber();

    long countByStatus(TicketStatus status);

    long countByCreatedBy_Id(Long userId);

    long countByCreatedBy_IdAndStatus(Long userId, TicketStatus status);

    long countByAssignedTo_Id(Long userId);
}
