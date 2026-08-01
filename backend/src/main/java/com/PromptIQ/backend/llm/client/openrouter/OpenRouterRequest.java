package com.PromptIQ.backend.llm.client.openrouter;

import java.util.List;

public record OpenRouterRequest(
        String model,
        List<Message> messages,
        Boolean stream,
        List<ToolDefinition> tools
) {
    public record Message(String role, String content, String tool_call_id, List<ToolCall> tool_calls) {
        public static Message of(String role, String content) {
            return new Message(role, content, null, null);
        }
    }

    public record ToolCall(String id, String type, FunctionCall function) {}
    public record FunctionCall(String name, String arguments) {}

    public record ToolDefinition(String type, FunctionDefinition function) {
        public static ToolDefinition function(String name, String description, String parametersSchema) {
            return new ToolDefinition("function", new FunctionDefinition(name, description, parametersSchema));
        }
    }
    public record FunctionDefinition(String name, String description, Object parameters) {
        public FunctionDefinition(String name, String description, String parametersJson) {
            this(name, description, parseSchema(parametersJson));
        }
        private static Object parseSchema(String json) {
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Object.class);
            } catch (Exception e) {
                throw new RuntimeException("Invalid tool parameter schema JSON", e);
            }
        }
    }

    public static OpenRouterRequest nonStreaming(String model, List<Message> messages, List<ToolDefinition> tools) {
        return new OpenRouterRequest(model, messages, false, tools == null || tools.isEmpty() ? null : tools);
    }

    public static OpenRouterRequest streaming(String model, List<Message> messages) {
        return new OpenRouterRequest(model, messages, true, null);
    }
}