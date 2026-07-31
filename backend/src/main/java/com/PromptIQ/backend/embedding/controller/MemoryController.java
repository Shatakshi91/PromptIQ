package com.PromptIQ.backend.embedding.controller;

import com.PromptIQ.backend.auth.security.UserPrincipal;
import com.PromptIQ.backend.embedding.dto.MemoryResponse;
import com.PromptIQ.backend.embedding.service.MemoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/memories")
public class MemoryController {

    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @GetMapping
    public List<MemoryResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return memoryService.listAll(principal.getId()).stream().map(MemoryResponse::from).toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        memoryService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}