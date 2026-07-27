package com.PromptIQ.backend.chat.dto;
import com.PromptIQ.backend.chat.entity.Message;
import com.PromptIQ.backend.chat.entity.MessageRole;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        MessageRole role,
        String content,
        Instant createdAt
) {
    public static MessageResponse from(Message m) {
        return new MessageResponse(
                m.getId(),
                m.getConversation().getId(),
                m.getRole(),
                m.getContent(),
                m.getCreatedAt()
        );
    }
}