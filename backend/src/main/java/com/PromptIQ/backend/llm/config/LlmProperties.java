package com.PromptIQ.backend.llm.config;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.llm")
public class LlmProperties {

    private String provider;
    private OpenRouter openrouter = new OpenRouter();
    private long requestTimeoutSeconds = 60;

    public static class OpenRouter {
        private String baseUrl;
        private String apiKey;
        private String model;
        private String referer;
        private String title;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getReferer() { return referer; }
        public void setReferer(String referer) { this.referer = referer; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
    }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public OpenRouter getOpenrouter() { return openrouter; }
    public void setOpenrouter(OpenRouter openrouter) { this.openrouter = openrouter; }
    public long getRequestTimeoutSeconds() { return requestTimeoutSeconds; }
    public void setRequestTimeoutSeconds(long requestTimeoutSeconds) { this.requestTimeoutSeconds = requestTimeoutSeconds; }
}