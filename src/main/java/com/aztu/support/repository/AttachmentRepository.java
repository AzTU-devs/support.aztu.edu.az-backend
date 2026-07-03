package com.aztu.support.repository;

import com.aztu.support.domain.Attachment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByTicket_IdOrderByCreatedAtAsc(Long ticketId);
}
