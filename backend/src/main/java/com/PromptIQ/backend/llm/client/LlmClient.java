package com.PromptIQ.backend.llm.client;

import reactor.core.publisher.Flux;
import java.util.List;

public interface LlmClient {

    LlmResponse chat(List<LlmMessage> messages);

    /** Overload that also advertises available tools to the model. */
    LlmResponse chat(List<LlmMessage> messages, List<ToolSpec> tools);

    Flux<String> streamChat(List<LlmMessage> messages);

    record LlmMessage(String role, String content, String toolCallId, List<ToolCallRequest> toolCalls) {
        public static LlmMessage user(String content) { return new LlmMessage("user", content, null, null); }
        public static LlmMessage assistant(String content) { return new LlmMessage("assistant", content, null, null); }
        public static LlmMessage system(String content) { return new LlmMessage("system", content, null, null); }
        public static LlmMessage assistantWithToolCalls(List<ToolCallRequest> toolCalls) {
            return new LlmMessage("assistant", null, null, toolCalls);
        }
        public static LlmMessage toolResult(String toolCallId, String content) {
            return new LlmMessage("tool", content, toolCallId, null);
        }
    }

    record ToolSpec(String name, String description, String parametersJsonSchema) {}

    record ToolCallRequest(String id, String toolName, String argumentsJson) {}

    record LlmResponse(String content, String model, int promptTokens, int completionTokens, List<ToolCallRequest> toolCalls) {
        public boolean hasToolCalls() { return toolCalls != null && !toolCalls.isEmpty(); }
    }
}