package com.PromptIQ.backend.embedding.client.openrouter;
import java.util.List;

public record OpenRouterEmbeddingResponse(List<Data> data) {
    public record Data(List<Float> embedding) {}
}