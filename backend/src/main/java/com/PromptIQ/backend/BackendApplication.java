package com.PromptIQ.backend;

import com.PromptIQ.backend.embedding.config.EmbeddingProperties;
import com.PromptIQ.backend.llm.config.LlmProperties;
import org.springframework.boot.SpringApplication;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({LlmProperties.class, EmbeddingProperties.class})
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}
}

