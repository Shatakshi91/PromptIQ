package com.PromptIQ.backend.document.dto;

import com.PromptIQ.backend.document.entity.Document;
import com.PromptIQ.backend.document.entity.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String filename,
        String contentType,
        Long fileSizeBytes,
        DocumentStatus status,
        String errorMessage,
        long chunkCount,
        Instant createdAt
) {
    public static DocumentResponse from(Document d, long chunkCount) {
        return new DocumentResponse(
                d.getId(), d.getFilename(), d.getContentType(), d.getFileSizeBytes(),
                d.getStatus(), d.getErrorMessage(), chunkCount, d.getCreatedAt()
        );
    }
}