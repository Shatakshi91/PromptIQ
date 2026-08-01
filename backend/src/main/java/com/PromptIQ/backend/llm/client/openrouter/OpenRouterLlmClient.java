package com.PromptIQ.backend.llm.client.openrouter;

import com.PromptIQ.backend.llm.client.LlmClient;
import com.PromptIQ.backend.llm.config.LlmProperties;
import com.PromptIQ.backend.common.exception.ApiException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

@Component
public class OpenRouterLlmClient implements LlmClient {

    private final WebClient webClient;
    private final LlmProperties properties;
    private final ObjectMapper objectMapper;

    public OpenRouterLlmClient(LlmProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(properties.getOpenrouter().getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getOpenrouter().getApiKey())
                .defaultHeader("HTTP-Referer", properties.getOpenrouter().getReferer())
                .defaultHeader("X-Title", properties.getOpenrouter().getTitle())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public LlmResponse chat(List<LlmMessage> messages) {
        return chat(messages, List.of());
    }

    @Override
    public LlmResponse chat(List<LlmMessage> messages, List<ToolSpec> tools) {
        List<OpenRouterRequest.ToolDefinition> toolDefs = tools.stream()
                .map(t -> OpenRouterRequest.ToolDefinition.function(t.name(), t.description(), t.parametersJsonSchema()))
                .toList();

        OpenRouterRequest request = OpenRouterRequest.nonStreaming(
                properties.getOpenrouter().getModel(),
                messages.stream().map(this::toOpenRouterMessage).toList(),
                toolDefs
        );

        try {
            OpenRouterResponse response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .defaultIfEmpty("(empty error body)")
                                    .flatMap(body -> {
                                        System.err.println("OpenRouter error response body: " + body);
                                        return reactor.core.publisher.Mono.error(
                                                new RuntimeException("OpenRouter error: " + body)
                                        );
                                    })
                    )
                    .bodyToMono(OpenRouterResponse.class)
                    .block(Duration.ofSeconds(properties.getRequestTimeoutSeconds()));

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "LLM provider returned an empty response");
            }

            OpenRouterResponse.Message message = response.choices().get(0).message();
            int promptTokens = response.usage() != null ? response.usage().prompt_tokens() : 0;
            int completionTokens = response.usage() != null ? response.usage().completion_tokens() : 0;

            List<ToolCallRequest> toolCallRequests = message.tool_calls() == null ? List.of()
                    : message.tool_calls().stream()
                    .map(tc -> new ToolCallRequest(tc.id(), tc.function().name(), tc.function().arguments()))
                    .toList();

            return new LlmResponse(message.content(), response.model(), promptTokens, completionTokens, toolCallRequests);

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Failed to reach LLM provider: " + e.getMessage());
        }
    }

    @Override
    public Flux<String> streamChat(List<LlmMessage> messages) {
        OpenRouterRequest request = OpenRouterRequest.streaming(
                properties.getOpenrouter().getModel(),
                messages.stream().map(this::toOpenRouterMessage).toList()
        );

        return webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .mapNotNull(ServerSentEvent::data)
                .filter(data -> data != null && !data.isBlank() && !data.equals("[DONE]"))
                .mapNotNull(this::parseChunk)
                .filter(content -> !content.isEmpty())
                .onErrorMap(e -> new ApiException(HttpStatus.BAD_GATEWAY, "Streaming failed: " + e.getMessage()));
    }

    private String parseChunk(String json) {
        try {
            OpenRouterStreamChunk chunk = objectMapper.readValue(json, OpenRouterStreamChunk.class);
            return chunk.extractContent();
        } catch (Exception e) {
            // A single malformed chunk shouldn't kill the whole stream — skip it and continue.
            return "";
        }
    }

    private OpenRouterRequest.Message toOpenRouterMessage(LlmMessage m) {
        if (m.toolCallId() != null) {
            return new OpenRouterRequest.Message("tool", m.content(), m.toolCallId(), null);
        }
        if (m.toolCalls() != null && !m.toolCalls().isEmpty()) {
            List<OpenRouterRequest.ToolCall> calls = m.toolCalls().stream()
                    .map(tc -> new OpenRouterRequest.ToolCall(tc.id(), "function",
                            new OpenRouterRequest.FunctionCall(tc.toolName(), tc.argumentsJson())))
                    .toList();
            return new OpenRouterRequest.Message("assistant", null, null, calls);
        }
        return OpenRouterRequest.Message.of(m.role(), m.content());
    }
}