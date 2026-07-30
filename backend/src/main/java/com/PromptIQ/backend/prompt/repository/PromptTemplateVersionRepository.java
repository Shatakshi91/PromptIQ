package com.PromptIQ.backend.prompt.repository;
import com.PromptIQ.backend.prompt.entity.PromptTemplateVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PromptTemplateVersionRepository extends JpaRepository<PromptTemplateVersion, UUID> {

    List<PromptTemplateVersion> findByPromptTemplateIdOrderByVersionNumberAsc(UUID promptTemplateId);
}