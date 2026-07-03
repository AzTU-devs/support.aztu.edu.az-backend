package com.aztu.support.dto.ticket;

import com.aztu.support.domain.Comment;
import com.aztu.support.domain.enums.RoleName;
import java.time.Instant;

public record CommentResponse(
        Long id,
        Long authorId,
        String authorName,
        RoleName authorRole,
        String body,
        boolean internal,
        Instant createdAt) {

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getAuthor().getId(),
                comment.getAuthor().fullName(),
                comment.getAuthor().roleName(),
                comment.getBody(),
                comment.isInternal(),
                comment.getCreatedAt());
    }
}
