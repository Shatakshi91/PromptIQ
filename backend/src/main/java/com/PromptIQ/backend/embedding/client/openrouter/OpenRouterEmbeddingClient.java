package com.PromptIQ.backend.embedding.client.openrouter;
import com.PromptIQ.backend.common.exception.ApiException;
import com.PromptIQ.backend.embedding.client.EmbeddingClient;
import com.PromptIQ.backend.embedding.config.EmbeddingProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;

import java.time.Duration;

@Component
public class OpenRouterEmbeddingClient implements EmbeddingClient {

    private final WebClient webClient;
    private final EmbeddingProperties properties;

    public OpenRouterEmbeddingClient(EmbeddingProperties properties) {
        this.properties = properties;
        this.webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public float[] embed(String text) {
        try {
            OpenRouterEmbeddingResponse response = webClient.post()
                    .uri("/embeddings")
                    .bodyValue(new OpenRouterEmbeddingRequest(properties.getModel(), text))
                    .retrieve()
                    .bodyToMono(OpenRouterEmbeddingResponse.class)
                    .block(Duration.ofSeconds(30));

            if (response == null || response.data() == null || response.data().isEmpty()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Embedding provider returned an empty response");
            }

            List<Float> vector = response.data().get(0).embedding();
            float[] result = new float[vector.size()];
            for (int i = 0; i < vector.size(); i++) result[i] = vector.get(i);
            return result;

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Failed to reach embedding provider: " + e.getMessage());
        }
    }

    @Override
    public int dimensions() {
        return properties.getDimensions();
    }
}