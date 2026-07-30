package com.PromptIQ.backend.prompt.service;
import com.PromptIQ.backend.auth.repository.UserRepository;
import com.PromptIQ.backend.common.dto.PageResponse;
import com.PromptIQ.backend.common.exception.ApiException;
import com.PromptIQ.backend.prompt.dto.*;
import com.PromptIQ.backend.prompt.entity.PromptTemplate;
import com.PromptIQ.backend.prompt.entity.PromptTemplateVersion;
import com.PromptIQ.backend.prompt.repository.PromptTemplateRepository;
import com.PromptIQ.backend.prompt.repository.PromptTemplateVersionRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PromptService {

    private final PromptTemplateRepository promptRepository;
    private final PromptTemplateVersionRepository versionRepository;
    private final UserRepository userRepository;

    public PromptService(
            PromptTemplateRepository promptRepository,
            PromptTemplateVersionRepository versionRepository,
            UserRepository userRepository
    ) {
        this.promptRepository = promptRepository;
        this.versionRepository = versionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PromptResponse create(UUID userId, CreatePromptRequest request) {
        if (request.isDefault()) {
            clearExistingDefault(userId);
        }

        PromptTemplate prompt = PromptTemplate.builder()
                .user(userRepository.getReferenceById(userId))
                .name(request.name())
                .description(request.description())
                .content(request.content())
                .isDefault(request.isDefault())
                .currentVersion(1)
                .build();

        prompt = promptRepository.save(prompt);
        saveVersionSnapshot(prompt, 1);

        return PromptResponse.from(prompt);
    }

    public PageResponse<PromptResponse> list(UUID userId, Pageable pageable) {
        return PageResponse.from(
                promptRepository.findByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(userId, pageable),
                PromptResponse::from
        );
    }

    public PromptResponse get(UUID userId, UUID promptId) {
        return PromptResponse.from(getOwnedPromptOrThrow(userId, promptId));
    }

    @Transactional
    public PromptResponse update(UUID userId, UUID promptId, UpdatePromptRequest request) {
        PromptTemplate prompt = getOwnedPromptOrThrow(userId, promptId);

        if (request.name() != null && !request.name().isBlank()) {
            prompt.setName(request.name());
        }
        if (request.description() != null) {
            prompt.setDescription(request.description());
        }

        boolean contentChanged = !prompt.getContent().equals(request.content());
        prompt.setContent(request.content());

        if (contentChanged) {
            int newVersion = prompt.getCurrentVersion() + 1;
            prompt.setCurrentVersion(newVersion);
            saveVersionSnapshot(prompt, newVersion);
        }

        return PromptResponse.from(promptRepository.save(prompt));
    }

    @Transactional
    public void softDelete(UUID userId, UUID promptId) {
        PromptTemplate prompt = getOwnedPromptOrThrow(userId, promptId);
        prompt.setDeletedAt(java.time.Instant.now());
        promptRepository.save(prompt);
    }

    @Transactional
    public PromptResponse setDefault(UUID userId, UUID promptId) {
        PromptTemplate prompt = getOwnedPromptOrThrow(userId, promptId);
        clearExistingDefault(userId);
        prompt.setDefault(true);
        return PromptResponse.from(promptRepository.save(prompt));
    }

    public List<PromptVersionResponse> listVersions(UUID userId, UUID promptId) {
        getOwnedPromptOrThrow(userId, promptId); // ownership check
        return versionRepository.findByPromptTemplateIdOrderByVersionNumberAsc(promptId)
                .stream().map(PromptVersionResponse::from).toList();
    }

    /**
     * Package-private accessor used by ChatOrchestrationService to resolve the
     * effective system prompt content for a conversation, without exposing full CRUD access.
     */
    public String resolveSystemPromptContent(UUID userId, PromptTemplate assignedPrompt) {
        if (assignedPrompt != null) {
            return assignedPrompt.getContent();
        }
        return promptRepository.findByUserIdAndIsDefaultTrueAndDeletedAtIsNull(userId)
                .map(PromptTemplate::getContent)
                .orElse("You are a helpful AI assistant.");
    }

    public PromptTemplate getOwnedPromptOrThrow(UUID userId, UUID promptId) {
        PromptTemplate prompt = promptRepository.findByIdAndDeletedAtIsNull(promptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Prompt not found"));

        if (!prompt.getUser().getId().equals(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Prompt not found");
        }
        return prompt;
    }

    private void clearExistingDefault(UUID userId) {
        promptRepository.findByUserIdAndIsDefaultTrueAndDeletedAtIsNull(userId)
                .ifPresent(existing -> {
                    existing.setDefault(false);
                    promptRepository.save(existing);
                });
    }

    private void saveVersionSnapshot(PromptTemplate prompt, int versionNumber) {
        PromptTemplateVersion version = PromptTemplateVersion.builder()
                .promptTemplate(prompt)
                .versionNumber(versionNumber)
                .content(prompt.getContent())
                .build();
        versionRepository.save(version);
    }
}