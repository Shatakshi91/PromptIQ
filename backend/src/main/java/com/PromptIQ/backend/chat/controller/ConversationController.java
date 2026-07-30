package com.PromptIQ.backend.chat.controller;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.PromptIQ.backend.chat.entity.MessageRole;
import org.springframework.http.MediaType;
import com.PromptIQ.backend.auth.security.UserPrincipal;
import com.PromptIQ.backend.chat.dto.*;
import com.PromptIQ.backend.chat.service.ChatOrchestrationService;
import com.PromptIQ.backend.chat.service.ConversationService;
import com.PromptIQ.backend.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final ChatOrchestrationService chatOrchestrationService;

    public ConversationController(ConversationService conversationService, ChatOrchestrationService chatOrchestrationService) {
        this.chatOrchestrationService = chatOrchestrationService;
        this.conversationService = conversationService;
    }

    @PostMapping
    public ResponseEntity<ConversationResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateConversationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(conversationService.create(principal.getId(), request));
    }

    @GetMapping
    public PageResponse<ConversationResponse> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return conversationService.list(principal.getId(), pageable);
    }

    @GetMapping("/{id}")
    public ConversationResponse get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        return conversationService.get(principal.getId(), id);
    }

    @PatchMapping("/{id}")
    public ConversationResponse rename(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateConversationRequest request
    ) {
        return conversationService.rename(principal.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        conversationService.softDelete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<MessageResponse> addMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody CreateMessageRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(conversationService.addMessage(principal.getId(), id, request));
    }

    @GetMapping("/{id}/messages")
    public PageResponse<MessageResponse> listMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return conversationService.listMessages(principal.getId(), id, pageable);
    }
    @PostMapping("/{id}/chat")
    public ResponseEntity<MessageResponse> chat(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequest request
    ) {
        return ResponseEntity.ok(
                chatOrchestrationService.sendUserMessageAndGetReply(principal.getId(), id, request.content())
        );
    }

    @PatchMapping("/{id}/prompt")
    public ConversationResponse assignPrompt(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody AssignPromptRequest request
    ) {
        return conversationService.assignPrompt(principal.getId(), id, request.promptTemplateId());
    }

    @PostMapping(value = "/{id}/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequest request
    ) {
        SseEmitter emitter = new SseEmitter(120_000L); // 2 min timeout — generous for long replies
        UUID userId = principal.getId();

        chatOrchestrationService.streamUserMessageAndGetReply(
                userId,
                id,
                request.content(),
                finalContent -> {
                    // Called exactly once, when the stream completes (success or handled error)
                    try {
                        conversationService.addMessage(
                                userId, id, new CreateMessageRequest(MessageRole.ASSISTANT, finalContent)
                        );
                        emitter.send(SseEmitter.event().name("done").data(""));
                        emitter.complete();
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                }
        ).subscribe(
                token -> {
                    try {
                        emitter.send(SseEmitter.event().name("token").data(token));
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                },
                emitter::completeWithError
        );

        emitter.onTimeout(() -> emitter.completeWithError(new RuntimeException("Stream timed out")));
        emitter.onError(e -> { /* already handled above; avoid double-completion */ });

        return emitter;
    }
}