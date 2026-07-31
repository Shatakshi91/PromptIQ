package com.PromptIQ.backend.chat.service;
import com.PromptIQ.backend.chat.dto.CreateMessageRequest;
import com.PromptIQ.backend.chat.dto.MessageResponse;
import com.PromptIQ.backend.chat.entity.Conversation;
import com.PromptIQ.backend.chat.entity.Message;
import com.PromptIQ.backend.chat.entity.MessageRole;
import com.PromptIQ.backend.chat.repository.MessageRepository;
import com.PromptIQ.backend.llm.client.LlmClient;
import com.PromptIQ.backend.prompt.service.PromptService;
import org.springframework.transaction.annotation.Transactional;
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
    private final PromptService promptService;

    public ChatOrchestrationService(
            ConversationService conversationService,
            MessageRepository messageRepository,
            LlmClient llmClient,
            PromptService promptService
    ) {
        this.conversationService = conversationService;
        this.messageRepository = messageRepository;
        this.llmClient = llmClient;
        this.promptService = promptService;
    }

    @Transactional
    public MessageResponse sendUserMessageAndGetReply(UUID userId, UUID conversationId, String userContent) {
        conversationService.addMessage(userId, conversationId, new CreateMessageRequest(MessageRole.USER, userContent));

        List<LlmClient.LlmMessage> llmMessages = buildHistory(userId, conversationId);
        LlmClient.LlmResponse llmResponse = llmClient.chat(llmMessages);

        return conversationService.addMessage(
                userId, conversationId, new CreateMessageRequest(MessageRole.ASSISTANT, llmResponse.content())
        );
    }

    @Transactional
    public Flux<String> streamUserMessageAndGetReply(
            UUID userId,
            UUID conversationId,
            String userContent,
            java.util.function.Consumer<String> onComplete
    ) {
        conversationService.addMessage(userId, conversationId, new CreateMessageRequest(MessageRole.USER, userContent));

        List<LlmClient.LlmMessage> llmMessages = buildHistory(userId, conversationId);

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

    private List<LlmClient.LlmMessage> buildHistory(UUID userId, UUID conversationId) {
        Conversation conversation = conversationService.getConversationEntity(userId, conversationId);
        String systemPromptContent = promptService.resolveSystemPromptContent(userId, conversation.getPromptTemplate());

        List<Message> recentMessages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(
                        conversationId,
                        PageRequest.of(0, MAX_HISTORY_MESSAGES, Sort.by("createdAt").ascending())
                )
                .getContent();

        List<LlmClient.LlmMessage> llmMessages = new java.util.ArrayList<>();
        llmMessages.add(LlmClient.LlmMessage.system(systemPromptContent));
        recentMessages.forEach(m -> llmMessages.add(new LlmClient.LlmMessage(mapRole(m.getRole()), m.getContent())));

        return llmMessages;
    }
    private String mapRole(MessageRole role) {
        return switch (role) {
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case SYSTEM -> "system";
        };
    }
}