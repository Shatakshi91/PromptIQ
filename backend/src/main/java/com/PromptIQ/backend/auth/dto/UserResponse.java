package com.PromptIQ.backend.auth.dto;
import com.PromptIQ.backend.auth.entity.Role;
import com.PromptIQ.backend.auth.entity.User;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String displayName,
        Role role,
        boolean enabled
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(), user.isEnabled());
    }
}