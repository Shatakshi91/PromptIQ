package com.PromptIQ.backend.chat.dto;
import jakarta.validation.constraints.Size;

public record CreateConversationRequest(
        @Size(max = 255) String title
) {}