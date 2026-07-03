package com.aztu.support.dto.ticket;

import com.aztu.support.domain.Attachment;
import java.time.Instant;

public record AttachmentResponse(
        Long id,
        String originalFilename,
        String contentType,
        Long fileSize,
        String uploadedByName,
        Instant createdAt) {

    public static AttachmentResponse from(Attachment a) {
        return new AttachmentResponse(
                a.getId(),
                a.getOriginalFilename(),
                a.getContentType(),
                a.getFileSize(),
                a.getUploadedBy().fullName(),
                a.getCreatedAt());
    }
}
