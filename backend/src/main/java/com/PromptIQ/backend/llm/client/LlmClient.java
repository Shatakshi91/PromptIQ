package com.PromptIQ.backend.llm.client;
import java.util.List;
public interface LlmClient {

    LlmResponse chat(List<LlmMessage> messages);

    record LlmMessage(String role, String content) {
        public static LlmMessage user(String content) { return new LlmMessage("user", content); }
        public static LlmMessage assistant(String content) { return new LlmMessage("assistant", content); }
        public static LlmMessage system(String content) { return new LlmMessage("system", content); }
    }

    record LlmResponse(String content, String model, int promptTokens, int completionTokens) {}
}