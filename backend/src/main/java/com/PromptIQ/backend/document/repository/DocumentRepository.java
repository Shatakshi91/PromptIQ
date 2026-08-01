package com.PromptIQ.backend.document.repository;

import com.PromptIQ.backend.document.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Page<Document> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<Document> findByIdAndUserId(UUID id, UUID userId);
}