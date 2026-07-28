package com.PromptIQ.backend.chat.service;
import com.PromptIQ.backend.chat.dto.CreateMessageRequest;
import com.PromptIQ.backend.chat.dto.MessageResponse;
import com.PromptIQ.backend.chat.entity.Message;
import com.PromptIQ.backend.chat.entity.MessageRole;
import com.PromptIQ.backend.chat.repository.MessageRepository;
import com.PromptIQ.backend.llm.client.LlmClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ChatOrchestrationService {

    private static final int MAX_HISTORY_MESSAGES = 20; // context window guard, revisited properly in Feature 8 (memory)

    private final ConversationService conversationService;
    private final MessageRepository messageRepository;
    private final LlmClient llmClient;

    public ChatOrchestrationService(
            ConversationService conversationService,
            MessageRepository messageRepository,
            LlmClient llmClient
    ) {
        this.conversationService = conversationService;
        this.messageRepository = messageRepository;
        this.llmClient = llmClient;
    }

    /**
     * Persists the user's message, calls the LLM with recent conversation history
     * for context, persists the assistant's reply, and returns it.
     */
    public MessageResponse sendUserMessageAndGetReply(UUID userId, UUID conversationId, String userContent) {
        // 1. Persist the user's message (reuses Feature 3's ownership-checked service)
        conversationService.addMessage(userId, conversationId, new CreateMessageRequest(MessageRole.USER, userContent));

        // 2. Build conversation history for context (oldest-first, capped)
        List<Message> recentMessages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(
                        conversationId,
                        PageRequest.of(0, MAX_HISTORY_MESSAGES, Sort.by("createdAt").ascending())
                )
                .getContent();

        List<LlmClient.LlmMessage> llmMessages = recentMessages.stream()
                .map(m -> new LlmClient.LlmMessage(mapRole(m.getRole()), m.getContent()))
                .toList();

        // 3. Call the LLM (provider-agnostic — this line doesn't know or care it's OpenRouter)
        LlmClient.LlmResponse llmResponse = llmClient.chat(llmMessages);

        // 4. Persist the assistant's reply
        return conversationService.addMessage(
                userId,
                conversationId,
                new CreateMessageRequest(MessageRole.ASSISTANT, llmResponse.content())
        );
    }

    private String mapRole(MessageRole role) {
        return switch (role) {
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case SYSTEM -> "system";
        };
    }
}