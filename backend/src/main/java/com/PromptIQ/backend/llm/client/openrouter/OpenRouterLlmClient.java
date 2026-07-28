package com.PromptIQ.backend.llm.client.openrouter;
import com.PromptIQ.backend.common.exception.ApiException;
import com.PromptIQ.backend.llm.client.LlmClient;
import com.PromptIQ.backend.llm.config.LlmProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

@Component
public class OpenRouterLlmClient implements LlmClient {

    private final WebClient webClient;
    private final LlmProperties properties;

    public OpenRouterLlmClient(LlmProperties properties) {
        this.properties = properties;
        System.out.println("DEBUG - FULL API key loaded: [" + properties.getOpenrouter().getApiKey() + "]");
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
        OpenRouterRequest request = new OpenRouterRequest(
                properties.getOpenrouter().getModel(),
                messages.stream()
                        .map(m -> new OpenRouterRequest.Message(m.role(), m.content()))
                        .toList()
        );

        try {
            OpenRouterResponse response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OpenRouterResponse.class)
                    .block(Duration.ofSeconds(properties.getRequestTimeoutSeconds()));

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "LLM provider returned an empty response");
            }

            String content = response.choices().get(0).message().content();
            int promptTokens = response.usage() != null ? response.usage().prompt_tokens() : 0;
            int completionTokens = response.usage() != null ? response.usage().completion_tokens() : 0;

            return new LlmResponse(content, response.model(), promptTokens, completionTokens);

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Failed to reach LLM provider: " + e.getMessage());
        }
    }
}