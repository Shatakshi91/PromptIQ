package com.PromptIQ.backend.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.PromptIQ.backend.llm.client.LlmClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ToolExecutionService {

    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ToolExecutionService(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    public List<LlmClient.ToolSpec> availableToolSpecs() {
        return toolRegistry.all().stream()
                .map(t -> new LlmClient.ToolSpec(t.name(), t.description(), t.parametersJsonSchema()))
                .toList();
    }

    /** Executes one requested tool call and returns its result as text, never throwing. */
    public String execute(LlmClient.ToolCallRequest request) {
        try {
            var tool = toolRegistry.find(request.toolName());
            if (tool.isEmpty()) {
                return "Error: unknown tool '" + request.toolName() + "'";
            }

            var arguments = objectMapper.readTree(
                    request.argumentsJson() == null || request.argumentsJson().isBlank() ? "{}" : request.argumentsJson()
            );

            return tool.get().execute(arguments);
        } catch (Exception e) {
            // A misbehaving tool should never crash the conversation — surface the error
            // as the tool's "result" so the LLM can explain the failure to the user.
            return "Error executing tool: " + e.getMessage();
        }
    }
}