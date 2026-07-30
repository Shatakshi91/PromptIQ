package com.PromptIQ.backend.prompt.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePromptRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 500) String description,
        @NotBlank String content,
        boolean isDefault
) {}