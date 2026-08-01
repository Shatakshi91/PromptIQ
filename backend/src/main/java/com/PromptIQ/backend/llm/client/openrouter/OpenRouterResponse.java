package com.PromptIQ.backend.llm.client.openrouter;

import java.util.List;

public record OpenRouterResponse(
        String model,
        List<Choice> choices,
        Usage usage
) {
    public record Choice(Message message) {}
    public record Message(String role, String content, List<OpenRouterRequest.ToolCall> tool_calls) {}
    public record Usage(int prompt_tokens, int completion_tokens, int total_tokens) {}
}