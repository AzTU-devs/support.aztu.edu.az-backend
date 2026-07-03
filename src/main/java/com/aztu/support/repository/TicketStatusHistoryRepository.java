package com.aztu.support.repository;

import com.aztu.support.domain.TicketStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketStatusHistoryRepository extends JpaRepository<TicketStatusHistory, Long> {
    List<TicketStatusHistory> findByTicket_IdOrderByCreatedAtAsc(Long ticketId);
}
