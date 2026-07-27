package com.PromptIQ.backend.chat.service;
import com.PromptIQ.backend.auth.entity.User;
import com.PromptIQ.backend.auth.repository.UserRepository;
import com.PromptIQ.backend.chat.dto.*;
import com.PromptIQ.backend.chat.entity.Conversation;
import com.PromptIQ.backend.chat.entity.Message;
import com.PromptIQ.backend.chat.repository.ConversationRepository;
import com.PromptIQ.backend.chat.repository.MessageRepository;
import com.PromptIQ.backend.common.dto.PageResponse;
import com.PromptIQ.backend.common.exception.ApiException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public ConversationService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            UserRepository userRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ConversationResponse create(UUID userId, CreateConversationRequest request) {
        User user = userRepository.getReferenceById(userId);

        Conversation conversation = Conversation.builder()
                .user(user)
                .title((request.title() == null || request.title().isBlank())
                        ? "New Conversation"
                        : request.title())
                .build();

        return ConversationResponse.from(conversationRepository.save(conversation));
    }

    public PageResponse<ConversationResponse> list(UUID userId, Pageable pageable) {
        return PageResponse.from(
                conversationRepository.findByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(userId, pageable),
                ConversationResponse::from
        );
    }

    public ConversationResponse get(UUID userId, UUID conversationId) {
        return ConversationResponse.from(getOwnedConversationOrThrow(userId, conversationId));
    }

    @Transactional
    public ConversationResponse rename(UUID userId, UUID conversationId, UpdateConversationRequest request) {
        Conversation conversation = getOwnedConversationOrThrow(userId, conversationId);
        conversation.setTitle(request.title());
        return ConversationResponse.from(conversationRepository.save(conversation));
    }

    @Transactional
    public void softDelete(UUID userId, UUID conversationId) {
        Conversation conversation = getOwnedConversationOrThrow(userId, conversationId);
        conversation.setDeletedAt(Instant.now());
        conversationRepository.save(conversation);
    }

    @Transactional
    public MessageResponse addMessage(UUID userId, UUID conversationId, CreateMessageRequest request) {
        Conversation conversation = getOwnedConversationOrThrow(userId, conversationId);

        Message message = Message.builder()
                .conversation(conversation)
                .role(request.role())
                .content(request.content())
                .build();

        messageRepository.save(message);

        conversation.touch();
        conversationRepository.save(conversation);

        return MessageResponse.from(message);
    }

    public PageResponse<MessageResponse> listMessages(UUID userId, UUID conversationId, Pageable pageable) {
        getOwnedConversationOrThrow(userId, conversationId); // ownership check, even for reads
        return PageResponse.from(
                messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId, pageable),
                MessageResponse::from
        );
    }

    /**
     * Central ownership guard. Every conversation-scoped operation goes through this.
     * Returns 404 (not 403) for both "doesn't exist" and "exists but isn't yours" —
     * this deliberately avoids leaking whether a given conversation ID exists at all
     * to a user who doesn't own it.
     */
    private Conversation getOwnedConversationOrThrow(UUID userId, UUID conversationId) {
        Conversation conversation = conversationRepository.findByIdAndDeletedAtIsNull(conversationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conversation not found"));

        if (!conversation.getUser().getId().equals(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Conversation not found");
        }

        return conversation;
    }
}