package com.PromptIQ.backend.chat.dto;
import com.PromptIQ.backend.chat.entity.MessageRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMessageRequest(
        @NotNull MessageRole role,
        @NotBlank String content
) {}