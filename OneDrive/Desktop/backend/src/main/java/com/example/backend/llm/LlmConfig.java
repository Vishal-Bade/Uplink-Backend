package com.example.backend.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LlmConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmConfig.class);

    @Value("${llm.provider:auto}")
    private String configuredProvider;

    @Value("${llm.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${llm.api-key:${LLM_API_KEY:${OPENAI_API_KEY:}}}")
    private String apiKey;

    @Value("${llm.model:gpt-4o-mini}")
    private String model;

    @Value("${llm.embedding-model:text-embedding-3-small}")
    private String embeddingModel;

    @Value("${llm.timeout-seconds:60}")
    private int timeoutSeconds;

    @Bean
    public LlmProvider llmProvider(ObjectMapper objectMapper) {
        if ("mock".equalsIgnoreCase(configuredProvider)) {
            log.info("Explicit mock LLM provider requested via configuration.");
            return new MockLlmProvider();
        }

        if (apiKey != null && !apiKey.isBlank()) {
            log.info("Active LLM API key detected. Using OpenAiCompatibleLlmProvider with model: {} and baseUrl: {}", model, baseUrl);
            return new OpenAiCompatibleLlmProvider(baseUrl, apiKey, model, embeddingModel, timeoutSeconds, objectMapper);
        }

        log.info("No LLM API key provided in environment (LLM_API_KEY/OPENAI_API_KEY). Defaulting to high-fidelity MockLlmProvider.");
        return new MockLlmProvider();
    }
}
