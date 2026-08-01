package com.PromptIQ.backend.workflow.dto;

import com.PromptIQ.backend.workflow.Workflow;

public record WorkflowInfoResponse(String key, String displayName, String description) {
    public static WorkflowInfoResponse from(Workflow w) {
        return new WorkflowInfoResponse(w.key(), w.displayName(), w.description());
    }
}