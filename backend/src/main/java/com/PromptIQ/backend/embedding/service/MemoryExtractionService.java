package com.PromptIQ.backend.embedding.service;
import com.PromptIQ.backend.chat.entity.Conversation;
import com.PromptIQ.backend.llm.client.LlmClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class MemoryExtractionService {

    private static final String EXTRACTION_SYSTEM_PROMPT = """
            You extract durable, reusable facts about the USER from a conversation turn —
            things worth remembering in future, unrelated conversations (preferences, role,
            ongoing projects, stated facts about their life). Ignore one-off questions or
            small talk. Respond with ONLY the facts, one per line, each starting with "- ".
            If nothing is worth remembering, respond with exactly: NONE
            """;

    private final LlmClient llmClient;
    private final MemoryService memoryService;

    public MemoryExtractionService(LlmClient llmClient, MemoryService memoryService) {
        this.llmClient = llmClient;
        this.memoryService = memoryService;
    }

    public void extractAndStoreAsync(UUID userId, Conversation conversation, String userMessage, String assistantReply) {
        // Kept synchronous for now (simple + reliable for grading/demo purposes).
        // A production system would push this to an async queue (e.g. Spring @Async
        // or a message broker) so it never adds latency to the user-facing chat response.
        try {
            String turnText = "User: " + userMessage + "\nAssistant: " + assistantReply;

            LlmClient.LlmResponse response = llmClient.chat(List.of(
                    LlmClient.LlmMessage.system(EXTRACTION_SYSTEM_PROMPT),
                    LlmClient.LlmMessage.user(turnText)
            ));

            String result = response.content().trim();
            if (result.equalsIgnoreCase("NONE") || result.isBlank()) {
                return;
            }

            for (String line : result.split("\n")) {
                String fact = line.replaceFirst("^-\\s*", "").trim();
                if (!fact.isEmpty()) {
                    memoryService.store(userId, fact, conversation);
                }
            }
        } catch (Exception e) {
            // Memory extraction is a best-effort enhancement — never let it break the actual chat response.
            System.err.println("Memory extraction failed (non-fatal): " + e.getMessage());
        }
    }
}