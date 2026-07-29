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
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ChatOrchestrationService {

    private static final int MAX_HISTORY_MESSAGES = 20;

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

    public MessageResponse sendUserMessageAndGetReply(UUID userId, UUID conversationId, String userContent) {
        conversationService.addMessage(userId, conversationId, new CreateMessageRequest(MessageRole.USER, userContent));

        List<LlmClient.LlmMessage> llmMessages = buildHistory(conversationId);
        LlmClient.LlmResponse llmResponse = llmClient.chat(llmMessages);

        return conversationService.addMessage(
                userId, conversationId, new CreateMessageRequest(MessageRole.ASSISTANT, llmResponse.content())
        );
    }

    /**
     * Streams the assistant's reply token-by-token. Persists the user message up front
     * (synchronously, before streaming starts), and persists the FULL accumulated assistant
     * reply once streaming completes — via the onComplete callback, not inside the Flux itself,
     * so a single DB write happens exactly once regardless of how many tokens arrived.
     *
     * @param onComplete called with the fully-assembled reply once the stream finishes,
     *                    so the controller can persist it and close the SSE connection.
     */
    public Flux<String> streamUserMessageAndGetReply(
            UUID userId,
            UUID conversationId,
            String userContent,
            java.util.function.Consumer<String> onComplete
    ) {
        conversationService.addMessage(userId, conversationId, new CreateMessageRequest(MessageRole.USER, userContent));

        List<LlmClient.LlmMessage> llmMessages = buildHistory(conversationId);

        AtomicReference<StringBuilder> accumulated = new AtomicReference<>(new StringBuilder());

        return llmClient.streamChat(llmMessages)
                .doOnNext(token -> accumulated.get().append(token))
                .doOnComplete(() -> onComplete.accept(accumulated.get().toString()))
                .doOnError(e -> {
                    // Persist whatever partial content we got, even on failure mid-stream,
                    // so the user doesn't lose a partially-generated answer entirely.
                    String partial = accumulated.get().toString();
                    if (!partial.isBlank()) {
                        onComplete.accept(partial + "\n\n[Response was interrupted]");
                    }
                });
    }

    private List<LlmClient.LlmMessage> buildHistory(UUID conversationId) {
        List<Message> recentMessages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(
                        conversationId,
                        PageRequest.of(0, MAX_HISTORY_MESSAGES, Sort.by("createdAt").ascending())
                )
                .getContent();

        return recentMessages.stream()
                .map(m -> new LlmClient.LlmMessage(mapRole(m.getRole()), m.getContent()))
                .toList();
    }

    private String mapRole(MessageRole role) {
        return switch (role) {
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case SYSTEM -> "system";
        };
    }
}