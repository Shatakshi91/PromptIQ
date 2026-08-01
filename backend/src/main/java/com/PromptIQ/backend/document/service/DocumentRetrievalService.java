package com.PromptIQ.backend.document.service;

import com.PromptIQ.backend.document.entity.DocumentChunk;
import com.PromptIQ.backend.document.repository.DocumentChunkRepository;
import com.PromptIQ.backend.embedding.client.EmbeddingClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class DocumentRetrievalService {

    private static final double MAX_DISTANCE = 0.85; // matched to MemoryService's tuned value
    private static final int TOP_K = 5;

    private final DocumentChunkRepository chunkRepository;
    private final EmbeddingClient embeddingClient;

    public DocumentRetrievalService(DocumentChunkRepository chunkRepository, EmbeddingClient embeddingClient) {
        this.chunkRepository = chunkRepository;
        this.embeddingClient = embeddingClient;
    }

    public List<String> retrieveRelevantChunks(UUID userId, String queryText) {
        float[] queryVector = embeddingClient.embed(queryText);
        String vectorLiteral = toVectorLiteral(queryVector);

        return chunkRepository.findSimilarChunks(userId, vectorLiteral, MAX_DISTANCE, TOP_K)
                .stream()
                .map(DocumentChunk::getContent)
                .collect(Collectors.toList());
    }

    private String toVectorLiteral(float[] vector) {
        return "[" + IntStream.range(0, vector.length)
                .mapToObj(i -> String.valueOf(vector[i]))
                .collect(Collectors.joining(",")) + "]";
    }
}