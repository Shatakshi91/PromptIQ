package com.PromptIQ.backend.prompt.dto;
import com.PromptIQ.backend.prompt.entity.PromptTemplate;

import java.time.Instant;
import java.util.UUID;

public record PromptResponse(
        UUID id,
        String name,
        String description,
        String content,
        boolean isDefault,
        int currentVersion,
        Instant createdAt,
        Instant updatedAt
) {
    public static PromptResponse from(PromptTemplate p) {
        return new PromptResponse(
                p.getId(), p.getName(), p.getDescription(), p.getContent(),
                p.isDefault(), p.getCurrentVersion(), p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}