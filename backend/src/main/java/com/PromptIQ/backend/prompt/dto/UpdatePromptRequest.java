package com.PromptIQ.backend.prompt.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdatePromptRequest(
        String name,
        String description,
        @NotBlank String content
) {}