package com.PromptIQ.backend.prompt.dto;
import com.PromptIQ.backend.prompt.entity.PromptTemplateVersion;

import java.time.Instant;

public record PromptVersionResponse(
        int versionNumber,
        String content,
        Instant createdAt
) {
    public static PromptVersionResponse from(PromptTemplateVersion v) {
        return new PromptVersionResponse(v.getVersionNumber(), v.getContent(), v.getCreatedAt());
    }
}