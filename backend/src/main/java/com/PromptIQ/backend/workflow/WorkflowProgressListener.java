package com.PromptIQ.backend.workflow;


public interface WorkflowProgressListener {
    void onStep(String stepDescription);
}