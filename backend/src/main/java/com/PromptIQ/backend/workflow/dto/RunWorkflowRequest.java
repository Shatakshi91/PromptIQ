package com.PromptIQ.backend.workflow.dto;

import jakarta.validation.constraints.NotBlank;

public record RunWorkflowRequest(@NotBlank String input) {}