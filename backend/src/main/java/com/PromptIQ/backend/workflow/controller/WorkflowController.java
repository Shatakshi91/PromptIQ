package com.PromptIQ.backend.workflow.controller;

import com.PromptIQ.backend.auth.security.UserPrincipal;
import com.PromptIQ.backend.chat.dto.MessageResponse;
import com.PromptIQ.backend.chat.dto.CreateMessageRequest;
import com.PromptIQ.backend.chat.entity.MessageRole;
import com.PromptIQ.backend.chat.service.ConversationService;
import com.PromptIQ.backend.common.exception.ApiException;
import com.PromptIQ.backend.workflow.WorkflowRegistry;
import com.PromptIQ.backend.workflow.dto.RunWorkflowRequest;
import com.PromptIQ.backend.workflow.dto.WorkflowInfoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class WorkflowController {

    private final WorkflowRegistry workflowRegistry;
    private final ConversationService conversationService;

    public WorkflowController(WorkflowRegistry workflowRegistry, ConversationService conversationService) {
        this.workflowRegistry = workflowRegistry;
        this.conversationService = conversationService;
    }

    @GetMapping("/api/v1/workflows")
    public List<WorkflowInfoResponse> list() {
        return workflowRegistry.all().stream().map(WorkflowInfoResponse::from).toList();
    }

    @PostMapping("/api/v1/conversations/{conversationId}/workflows/{key}")
    public MessageResponse run(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID conversationId,
            @PathVariable String key,
            @Valid @RequestBody RunWorkflowRequest request
    ) {
        var workflow = workflowRegistry.find(key)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Unknown workflow: " + key));

        // Ownership check happens implicitly via addMessage below (reuses Feature 3's guard),
        // but we verify explicitly first so we don't run an expensive multi-step workflow
        // only to fail on persistence at the very end.
        conversationService.get(principal.getId(), conversationId);

        conversationService.addMessage(principal.getId(), conversationId,
                new CreateMessageRequest(MessageRole.USER, "[Workflow: " + workflow.displayName() + "] " + request.input()));

        String result = workflow.run(principal.getId(), request.input(), stepDescription ->
                System.out.println("Workflow step [" + workflow.key() + "]: " + stepDescription));

        return conversationService.addMessage(principal.getId(), conversationId,
                new CreateMessageRequest(MessageRole.ASSISTANT, result));
    }
}