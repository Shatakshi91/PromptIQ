package com.PromptIQ.backend.document.repository;

import com.PromptIQ.backend.document.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    long countByDocumentId(UUID documentId);
    void deleteAllByDocumentId(UUID documentId);
    @Query(value = """
            SELECT dc.* FROM document_chunks dc
            JOIN documents d ON dc.document_id = d.id
            WHERE d.user_id = :userId
            AND d.status = 'PROCESSED'
            AND (dc.embedding <=> CAST(:queryVector AS vector)) < :maxDistance
            ORDER BY dc.embedding <=> CAST(:queryVector AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<DocumentChunk> findSimilarChunks(
            @Param("userId") UUID userId,
            @Param("queryVector") String queryVector,
            @Param("maxDistance") double maxDistance,
            @Param("limit") int limit
    );
}