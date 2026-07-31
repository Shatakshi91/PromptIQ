package com.PromptIQ.backend.embedding.dto;
import com.PromptIQ.backend.embedding.entity.UserMemory;

import java.time.Instant;
import java.util.UUID;

public record MemoryResponse(UUID id, String content, Instant createdAt) {
    public static MemoryResponse from(UserMemory m) {
        return new MemoryResponse(m.getId(), m.getContent(), m.getCreatedAt());
    }
}