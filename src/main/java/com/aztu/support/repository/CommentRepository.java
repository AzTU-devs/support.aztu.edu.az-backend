package com.aztu.support.repository;

import com.aztu.support.domain.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByTicket_IdOrderByCreatedAtAsc(Long ticketId);

    List<Comment> findByTicket_IdAndInternalFalseOrderByCreatedAtAsc(Long ticketId);
}
