package com.PromptIQ.backend.tool;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ToolRegistry {

    private final Map<String, Tool> toolsByName;

    public ToolRegistry(List<Tool> tools) {
        // Spring injects every @Component implementing Tool automatically — registering a new
        // tool is purely "write the class," no manual wiring needed here.
        this.toolsByName = tools.stream().collect(Collectors.toMap(Tool::name, t -> t));
    }

    public List<Tool> all() {
        return List.copyOf(toolsByName.values());
    }

    public Optional<Tool> find(String name) {
        return Optional.ofNullable(toolsByName.get(name));
    }
}