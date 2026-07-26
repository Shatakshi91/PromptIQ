package com.PromptIQ.backend.user.dto;
import com.PromptIQ.backend.auth.entity.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(@NotNull Role role) {}