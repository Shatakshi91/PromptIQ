package com.PromptIQ.backend.embedding.service;
import com.PromptIQ.backend.auth.repository.UserRepository;
import com.PromptIQ.backend.chat.entity.Conversation;
import com.PromptIQ.backend.embedding.client.EmbeddingClient;
import com.PromptIQ.backend.embedding.entity.UserMemory;
import com.PromptIQ.backend.embedding.repository.UserMemoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class MemoryService {

    private static final double MAX_DISTANCE = 0.85; // tuned for embedding similarity; 0.85 allows question-fact matches
    private static final int TOP_K = 5;

    private final UserMemoryRepository memoryRepository;
    private final EmbeddingClient embeddingClient;
    private final UserRepository userRepository;

    public MemoryService(UserMemoryRepository memoryRepository, EmbeddingClient embeddingClient, UserRepository userRepository) {
        this.memoryRepository = memoryRepository;
        this.embeddingClient = embeddingClient;
        this.userRepository = userRepository;
    }

    @Transactional
    public void store(UUID userId, String content, Conversation sourceConversation) {
        float[] vector = embeddingClient.embed(content);

        UserMemory memory = UserMemory.builder()
                .user(userRepository.getReferenceById(userId))
                .content(content)
                .embedding(vector)
                .sourceConversation(sourceConversation)
                .build();

        memoryRepository.save(memory);
    }

    public List<String> retrieveRelevant(UUID userId, String queryText) {
        float[] queryVector = embeddingClient.embed(queryText);
        String vectorLiteral = toVectorLiteral(queryVector);

        List<UserMemory> results = memoryRepository.findSimilar(userId, vectorLiteral, MAX_DISTANCE, TOP_K);
        System.out.println("DEBUG - memory search for [" + queryText + "] found " + results.size() + " matches");

        return results.stream().map(UserMemory::getContent).collect(Collectors.toList());
    }

    public List<UserMemory> listAll(UUID userId) {
        return memoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void delete(UUID userId, UUID memoryId) {
        UserMemory memory = memoryRepository.findById(memoryId)
                .filter(m -> m.getUser().getId().equals(userId))
                .orElseThrow(() -> new com.PromptIQ.backend.common.exception.ApiException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Memory not found"));
        memoryRepository.delete(memory);
    }

    private String toVectorLiteral(float[] vector) {
        return "[" + IntStream.range(0, vector.length)
                .mapToObj(i -> String.valueOf(vector[i]))
                .collect(Collectors.joining(",")) + "]";
    }
}