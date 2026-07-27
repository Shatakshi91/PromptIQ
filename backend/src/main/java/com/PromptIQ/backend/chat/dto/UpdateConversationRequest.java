package com.PromptIQ.backend.chat.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateConversationRequest(
        @NotBlank @Size(max = 255) String title
) {}