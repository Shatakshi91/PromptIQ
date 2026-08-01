package com.PromptIQ.backend.document.controller;

import com.PromptIQ.backend.auth.security.UserPrincipal;
import com.PromptIQ.backend.common.dto.PageResponse;
import com.PromptIQ.backend.document.dto.DocumentResponse;
import com.PromptIQ.backend.document.service.DocumentService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<DocumentResponse> upload(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.upload(principal.getId(), file));
    }

    @GetMapping
    public PageResponse<DocumentResponse> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return documentService.list(principal.getId(), pageable);
    }

    @GetMapping("/{id}")
    public DocumentResponse get(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        return documentService.get(principal.getId(), id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        documentService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}