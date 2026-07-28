package com.PromptIQ.backend.llm.client.openrouter;
import java.util.List;

public record OpenRouterRequest(
        String model,
        List<Message> messages
) {
    public record Message(String role, String content) {}
}