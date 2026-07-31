package com.PromptIQ.backend.embedding.repository;
import com.PromptIQ.backend.embedding.entity.UserMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserMemoryRepository extends JpaRepository<UserMemory, UUID> {

    List<UserMemory> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Cosine-distance similarity search: pgvector's <=> operator returns distance
     * (0 = identical, 2 = opposite), so we order ascending and cap with a threshold
     * to avoid returning irrelevant memories just because they're "least bad."
     */
    @Query(value = """
            SELECT * FROM user_memories
            WHERE user_id = :userId
            AND (embedding <=> CAST(:queryVector AS vector)) < :maxDistance
            ORDER BY embedding <=> CAST(:queryVector AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<UserMemory> findSimilar(
            @Param("userId") UUID userId,
            @Param("queryVector") String queryVector,
            @Param("maxDistance") double maxDistance,
            @Param("limit") int limit
    );
}