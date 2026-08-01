package com.PromptIQ.backend.chat.service;
import com.PromptIQ.backend.chat.dto.CreateMessageRequest;
import com.PromptIQ.backend.chat.dto.MessageResponse;
import com.PromptIQ.backend.chat.entity.Conversation;
import com.PromptIQ.backend.chat.entity.Message;
import com.PromptIQ.backend.chat.entity.MessageRole;
import com.PromptIQ.backend.chat.repository.MessageRepository;
import com.PromptIQ.backend.document.service.DocumentRetrievalService;
import com.PromptIQ.backend.embedding.service.MemoryExtractionService;
import com.PromptIQ.backend.embedding.service.MemoryService;
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
    private final ConversationSummaryService summaryService;
    private final MemoryService memoryService;
    private final MemoryExtractionService memoryExtractionService;
    private final DocumentRetrievalService documentRetrievalService;

    public ChatOrchestrationService(
            ConversationService conversationService,
            MessageRepository messageRepository,
            LlmClient llmClient,
            PromptService promptService,
            ConversationSummaryService summaryService,
            MemoryService memoryService,
            MemoryExtractionService memoryExtractionService,
            DocumentRetrievalService documentRetrievalService
    ) {
        this.conversationService = conversationService;
        this.messageRepository = messageRepository;
        this.llmClient = llmClient;
        this.promptService = promptService;
        this.summaryService = summaryService;
        this.memoryService = memoryService;
        this.memoryExtractionService = memoryExtractionService;
        this.documentRetrievalService = documentRetrievalService;

    }

    @Transactional
    public MessageResponse sendUserMessageAndGetReply(UUID userId, UUID conversationId, String userContent) {
        conversationService.addMessage(userId, conversationId, new CreateMessageRequest(MessageRole.USER, userContent));

        List<LlmClient.LlmMessage> llmMessages = buildHistory(userId, conversationId, userContent);
        LlmClient.LlmResponse llmResponse = llmClient.chat(llmMessages);

        MessageResponse assistantMessage = conversationService.addMessage(
                userId, conversationId, new CreateMessageRequest(MessageRole.ASSISTANT, llmResponse.content())
        );

        Conversation conversation = conversationService.getConversationEntity(userId, conversationId);
        memoryExtractionService.extractAndStoreAsync(userId, conversation, userContent, llmResponse.content());

        return assistantMessage;
    }

    @Transactional
    public Flux<String> streamUserMessageAndGetReply(
            UUID userId,
            UUID conversationId,
            String userContent,
            java.util.function.Consumer<String> onComplete
    ) {
        conversationService.addMessage(userId, conversationId, new CreateMessageRequest(MessageRole.USER, userContent));

        List<LlmClient.LlmMessage> llmMessages = buildHistory(userId, conversationId,userContent);

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

    private List<LlmClient.LlmMessage> buildHistory(UUID userId, UUID conversationId, String latestUserMessage) {
        Conversation conversation = conversationService.getConversationEntity(userId, conversationId);
        String systemPromptContent = promptService.resolveSystemPromptContent(userId, conversation.getPromptTemplate());

        // Long-term memory: retrieve relevant facts about this user
        // Long-term memory: retrieve relevant facts about this user
        List<String> relevantMemories = memoryService.retrieveRelevant(userId, latestUserMessage);
        if (!relevantMemories.isEmpty()) {
            systemPromptContent += "\n\nRelevant things you know about this user:\n"
                    + String.join("\n", relevantMemories.stream().map(m -> "- " + m).toList());
        }

// RAG: retrieve relevant chunks from the user's uploaded documents
        List<String> relevantChunks = documentRetrievalService.retrieveRelevantChunks(userId, latestUserMessage);
        if (!relevantChunks.isEmpty()) {
            systemPromptContent += "\n\nRelevant excerpts from the user's uploaded documents:\n"
                    + String.join("\n---\n", relevantChunks);
            systemPromptContent += "\n\nWhen your answer relies on these excerpts, mention that you're referencing the user's uploaded document.";
        }

        // Short-term memory: rolling summary + recent window, instead of a hard message cap
        ConversationSummaryService.ConversationContext context = summaryService.buildContext(conversation);
        if (context.summary() != null) {
            systemPromptContent += "\n\nSummary of earlier conversation:\n" + context.summary();
        }

        List<LlmClient.LlmMessage> llmMessages = new java.util.ArrayList<>();
        llmMessages.add(LlmClient.LlmMessage.system(systemPromptContent));
        context.recentMessages().forEach(m ->
                llmMessages.add(new LlmClient.LlmMessage(mapRole(m.getRole()), m.getContent())));

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