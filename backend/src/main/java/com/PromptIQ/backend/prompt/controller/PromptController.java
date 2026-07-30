package com.PromptIQ.backend.prompt.controller;
import com.PromptIQ.backend.auth.security.UserPrincipal;
import com.PromptIQ.backend.common.dto.PageResponse;
import com.PromptIQ.backend.prompt.dto.*;
import com.PromptIQ.backend.prompt.service.PromptService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prompts")
public class PromptController {

    private final PromptService promptService;

    public PromptController(PromptService promptService) {
        this.promptService = promptService;
    }

    @PostMapping
    public ResponseEntity<PromptResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreatePromptRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(promptService.create(principal.getId(), request));
    }

    @GetMapping
    public PageResponse<PromptResponse> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return promptService.list(principal.getId(), pageable);
    }

    @GetMapping("/{id}")
    public PromptResponse get(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        return promptService.get(principal.getId(), id);
    }

    @PatchMapping("/{id}")
    public PromptResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePromptRequest request
    ) {
        return promptService.update(principal.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        promptService.softDelete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/set-default")
    public PromptResponse setDefault(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        return promptService.setDefault(principal.getId(), id);
    }

    @GetMapping("/{id}/versions")
    public List<PromptVersionResponse> versions(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        return promptService.listVersions(principal.getId(), id);
    }
}