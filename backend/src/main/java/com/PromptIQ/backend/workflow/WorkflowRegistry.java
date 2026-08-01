package com.PromptIQ.backend.workflow;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class WorkflowRegistry {

    private final Map<String, Workflow> workflowsByKey;

    public WorkflowRegistry(List<Workflow> workflows) {
        this.workflowsByKey = workflows.stream().collect(Collectors.toMap(Workflow::key, w -> w));
    }

    public List<Workflow> all() {
        return List.copyOf(workflowsByKey.values());
    }

    public Optional<Workflow> find(String key) {
        return Optional.ofNullable(workflowsByKey.get(key));
    }
}