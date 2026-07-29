package com.PromptIQ.backend.llm.client.openrouter;
import java.util.List;

public record OpenRouterRequest(
        String model,
        List<Message> messages,
        Boolean stream
) {
    public record Message(String role, String content) {}

    public static OpenRouterRequest nonStreaming(String model, List<Message> messages) {
        return new OpenRouterRequest(model, messages, null);
    }

    public static OpenRouterRequest streaming(String model, List<Message> messages) {
        return new OpenRouterRequest(model, messages, true);
    }
}