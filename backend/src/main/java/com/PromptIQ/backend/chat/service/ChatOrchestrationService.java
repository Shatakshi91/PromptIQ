package com.PromptIQ.backend.chat.service;

import com.PromptIQ.backend.chat.dto.CreateMessageRequest;
import com.PromptIQ.backend.chat.dto.MessageResponse;
import com.PromptIQ.backend.chat.entity.Conversation;
import com.PromptIQ.backend.chat.entity.MessageRole;
import com.PromptIQ.backend.document.service.DocumentRetrievalService;
import com.PromptIQ.backend.embedding.service.MemoryExtractionService;
import com.PromptIQ.backend.embedding.service.MemoryService;
import com.PromptIQ.backend.llm.client.LlmClient;
import com.PromptIQ.backend.prompt.service.PromptService;
import com.PromptIQ.backend.tool.ToolExecutionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ChatOrchestrationService {

    private static final int MAX_TOOL_ITERATIONS = 3;

    private final ConversationService conversationService;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ConversationSummaryService summaryService;
    private final MemoryService memoryService;
    private final MemoryExtractionService memoryExtractionService;
    private final DocumentRetrievalService documentRetrievalService;
    private final ToolExecutionService toolExecutionService;

    public ChatOrchestrationService(
            ConversationService conversationService,
            LlmClient llmClient,
            PromptService promptService,
            ConversationSummaryService summaryService,
            MemoryService memoryService,
            MemoryExtractionService memoryExtractionService,
            DocumentRetrievalService documentRetrievalService,
            ToolExecutionService toolExecutionService
    ) {
        this.conversationService = conversationService;
        this.llmClient = llmClient;
        this.promptService = promptService;
        this.summaryService = summaryService;
        this.memoryService = memoryService;
        this.memoryExtractionService = memoryExtractionService;
        this.documentRetrievalService = documentRetrievalService;
        this.toolExecutionService = toolExecutionService;
    }

    @Transactional
    public MessageResponse sendUserMessageAndGetReply(UUID userId, UUID conversationId, String userContent) {
        conversationService.addMessage(userId, conversationId, new CreateMessageRequest(MessageRole.USER, userContent));

        List<LlmClient.LlmMessage> llmMessages = buildHistory(userId, conversationId, userContent);
        List<LlmClient.ToolSpec> tools = toolExecutionService.availableToolSpecs();

        LlmClient.LlmResponse llmResponse = llmClient.chat(llmMessages, tools);

        int iterations = 0;
        while (llmResponse.hasToolCalls() && iterations < MAX_TOOL_ITERATIONS) {
            llmMessages.add(LlmClient.LlmMessage.assistantWithToolCalls(llmResponse.toolCalls()));

            for (LlmClient.ToolCallRequest toolCall : llmResponse.toolCalls()) {
                String result = toolExecutionService.execute(toolCall);
                llmMessages.add(LlmClient.LlmMessage.toolResult(toolCall.id(), result));

                conversationService.addMessage(userId, conversationId, new CreateMessageRequest(
                        MessageRole.TOOL,
                        "[" + toolCall.toolName() + "(" + toolCall.argumentsJson() + ") \u2192 " + result + "]"
                ));
            }

            llmResponse = llmClient.chat(llmMessages, tools);
            iterations++;
        }

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

        List<LlmClient.LlmMessage> llmMessages = buildHistory(userId, conversationId, userContent);

        AtomicReference<StringBuilder> accumulated = new AtomicReference<>(new StringBuilder());

        return llmClient.streamChat(llmMessages)
                .doOnNext(token -> accumulated.get().append(token))
                .doOnComplete(() -> onComplete.accept(accumulated.get().toString()))
                .doOnError(e -> {
                    String partial = accumulated.get().toString();
                    if (!partial.isBlank()) {
                        onComplete.accept(partial + "\n\n[Response was interrupted]");
                    }
                });
    }

    private List<LlmClient.LlmMessage> buildHistory(UUID userId, UUID conversationId, String latestUserMessage) {
        Conversation conversation = conversationService.getConversationEntity(userId, conversationId);
        String systemPromptContent = promptService.resolveSystemPromptContent(userId, conversation.getPromptTemplate());

        List<String> relevantMemories = memoryService.retrieveRelevant(userId, latestUserMessage);
        if (!relevantMemories.isEmpty()) {
            systemPromptContent += "\n\nRelevant things you know about this user:\n"
                    + String.join("\n", relevantMemories.stream().map(m -> "- " + m).toList());
        }

        List<String> relevantChunks = documentRetrievalService.retrieveRelevantChunks(userId, latestUserMessage);
        if (!relevantChunks.isEmpty()) {
            systemPromptContent += "\n\nRelevant excerpts from the user's uploaded documents:\n"
                    + String.join("\n---\n", relevantChunks);
            systemPromptContent += "\n\nWhen your answer relies on these excerpts, mention that you're referencing the user's uploaded document.";
        }

        ConversationSummaryService.ConversationContext context = summaryService.buildContext(conversation);
        if (context.summary() != null) {
            systemPromptContent += "\n\nSummary of earlier conversation:\n" + context.summary();
        }

        List<LlmClient.LlmMessage> llmMessages = new java.util.ArrayList<>();
        llmMessages.add(LlmClient.LlmMessage.system(systemPromptContent));
        context.recentMessages().forEach(m -> {
            if (m.getRole() == MessageRole.TOOL) {
                llmMessages.add(new LlmClient.LlmMessage("user", "[Tool result from earlier: " + m.getContent() + "]", null, null));
            } else {
                llmMessages.add(new LlmClient.LlmMessage(mapRole(m.getRole()), m.getContent(), null, null));
            }
        });

        return llmMessages;
    }

    private String mapRole(MessageRole role) {
        return switch (role) {
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case SYSTEM -> "system";
            case TOOL -> "tool";
        };
    }
}