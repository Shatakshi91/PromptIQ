package com.PromptIQ.backend.llm.client.openrouter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenRouterStreamChunk(List<Choice> choices) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(Delta delta) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Delta(String content) {}

    public String extractContent() {
        if (choices == null || choices.isEmpty() || choices.get(0).delta() == null) {
            return "";
        }
        String content = choices.get(0).delta().content();
        return content != null ? content : "";
    }
}