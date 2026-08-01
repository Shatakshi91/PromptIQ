package com.PromptIQ.backend.tool;

import com.fasterxml.jackson.databind.JsonNode;

public interface Tool {
    String name();
    String description();
    String parametersJsonSchema();
    String execute(JsonNode arguments);
}