package com.PromptIQ.backend.prompt.repository;

import com.PromptIQ.backend.prompt.entity.PromptTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, UUID> {

    Page<PromptTemplate> findByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(UUID userId, Pageable pageable);

    Optional<PromptTemplate> findByIdAndDeletedAtIsNull(UUID id);

    Optional<PromptTemplate> findByUserIdAndIsDefaultTrueAndDeletedAtIsNull(UUID userId);
}