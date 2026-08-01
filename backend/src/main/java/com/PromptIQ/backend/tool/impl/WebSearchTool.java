package com.PromptIQ.backend.tool.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.PromptIQ.backend.tool.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Component
public class WebSearchTool implements Tool {

    private final WebClient webClient;
    private final boolean enabled;

    public WebSearchTool(@Value("${app.tools.tavily.api-key:}") String apiKey) {
        this.enabled = apiKey != null && !apiKey.isBlank();
        this.webClient = WebClient.builder()
                .baseUrl("https://api.tavily.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
        this.apiKey = apiKey;
    }

    private final String apiKey;

    @Override
    public String name() { return "web_search"; }

    @Override
    public String description() {
        return "Searches the web for current, real-world information not in your training data — "
                + "recent events, current prices, live facts. Use only when genuinely needed.";
    }

    @Override
    public String parametersJsonSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "query": { "type": "string", "description": "The search query" }
                  },
                  "required": ["query"]
                }
                """;
    }

    @Override
    public String execute(JsonNode arguments) {
        if (!enabled) {
            return "Web search is not configured on this server. Answer using existing knowledge and "
                    + "tell the user real-time web search is currently unavailable.";
        }

        String query = arguments.get("query").asText();

        try {
            String response = webClient.post()
                    .uri("/search")
                    .bodyValue(java.util.Map.of("api_key", apiKey, "query", query, "max_results", 3))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(15));
            return response != null ? response : "No results found.";
        } catch (Exception e) {
            return "Web search failed: " + e.getMessage();
        }
    }
}