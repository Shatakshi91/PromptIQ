package com.PromptIQ.backend.workflow;

import java.util.UUID;

public interface Workflow {


    String key();

    String displayName();

    String description();


    String run(UUID userId, String input, WorkflowProgressListener progressListener);
}