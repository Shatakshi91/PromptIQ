package com.PromptIQ.backend.embedding.client;
import java.util.List;

public interface EmbeddingClient {
    float[] embed(String text);
    int dimensions();
}