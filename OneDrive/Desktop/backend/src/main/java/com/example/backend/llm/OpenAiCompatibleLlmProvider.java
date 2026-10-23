package com.example.backend.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenAiCompatibleLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmProvider.class);

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final String embeddingModel;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleLlmProvider(String baseUrl, String apiKey, String model, String embeddingModel, int timeoutSeconds, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl != null && !baseUrl.isBlank() ? baseUrl.replaceAll("/+$", "") : "https://api.openai.com/v1";
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model != null && !model.isBlank() ? model : "gpt-4o-mini";
        this.embeddingModel = embeddingModel != null && !embeddingModel.isBlank() ? embeddingModel : "text-embedding-3-small";
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds > 0 ? timeoutSeconds : 30));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds > 0 ? timeoutSeconds : 60));

        this.restClient = RestClient.builder()
                .baseUrl(this.baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        log.info("Initialized OpenAiCompatibleLlmProvider with baseUrl={}, model={}, apiKeyMasked={}***",
                this.baseUrl, this.model, this.apiKey.length() > 6 ? this.apiKey.substring(0, 4) : "empty");
    }

    @Override
    public LlmResponse<String> generateText(String prompt, String systemInstruction) {
        Map<String, Object> requestBody = buildChatPayload(prompt, systemInstruction, false);
        return executeChatCompletion(requestBody, String.class);
    }

    @Override
    public <T> LlmResponse<T> generateStructured(String prompt, String systemInstruction, Class<T> responseClass) {
        String structuredSystemPrompt = (systemInstruction != null ? systemInstruction : "") +
                "\n\nIMPORTANT: You must respond ONLY with valid JSON conforming to the requested schema. Do not include markdown codeblocks or extra explanations.";

        Map<String, Object> requestBody = buildChatPayload(prompt, structuredSystemPrompt, true);
        LlmResponse<String> rawResponse = executeChatCompletion(requestBody, String.class);

        try {
            String jsonContent = extractJson(rawResponse.data());
            T parsedData = objectMapper.readValue(jsonContent, responseClass);
            return new LlmResponse<>(parsedData, rawResponse.promptTokens(), rawResponse.completionTokens(), rawResponse.totalTokens(), rawResponse.model());
        } catch (Exception e) {
            log.error("Failed to parse structured model response as {}: {}", responseClass.getSimpleName(), e.getMessage());
            throw new RuntimeException("Model returned malformed JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public float[] generateEmbedding(String text) {
        Map<String, Object> body = Map.of(
                "input", text != null ? text.trim() : "",
                "model", embeddingModel
        );

        int maxRetries = 3;
        long backoffMs = 1000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String responseStr = restClient.post()
                        .uri("/embeddings")
                        .body(body)
                        .retrieve()
                        .body(String.class);

                JsonNode root = objectMapper.readTree(responseStr);
                JsonNode dataArray = root.path("data");
                if (dataArray.isArray() && !dataArray.isEmpty()) {
                    JsonNode embeddingNode = dataArray.get(0).path("embedding");
                    if (embeddingNode.isArray()) {
                        float[] result = new float[embeddingNode.size()];
                        for (int i = 0; i < embeddingNode.size(); i++) {
                            result[i] = (float) embeddingNode.get(i).asDouble();
                        }
                        return result;
                    }
                }
                break;
            } catch (HttpClientErrorException.TooManyRequests | HttpServerErrorException e) {
                log.warn("Embedding API transient error on attempt {}/{}: {}", attempt, maxRetries, e.getMessage());
                if (attempt == maxRetries) throw new RuntimeException("Embedding API failed after retries", e);
                sleep(backoffMs);
                backoffMs *= 2;
            } catch (Exception e) {
                log.error("Embedding API error: {}", e.getMessage());
                throw new RuntimeException("Failed to generate embedding: " + e.getMessage(), e);
            }
        }
        return new float[0];
    }

    @Override
    public String getProviderName() {
        return "openai-compatible (" + model + ")";
    }

    private Map<String, Object> buildChatPayload(String prompt, String systemInstruction, boolean jsonMode) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (systemInstruction != null && !systemInstruction.isBlank()) {
            messages.add(Map.of("role", "system", "content", systemInstruction));
        }
        messages.add(Map.of("role", "user", "content", prompt));

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.2);

        if (jsonMode) {
            body.put("response_format", Map.of("type", "json_object"));
        }
        return body;
    }

    private LlmResponse<String> executeChatCompletion(Map<String, Object> requestBody, Class<?> targetClass) {
        int maxRetries = 3;
        long backoffMs = 1500;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String responseJson = restClient.post()
                        .uri("/chat/completions")
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

                JsonNode root = objectMapper.readTree(responseJson);
                String content = root.path("choices").get(0).path("message").path("content").asText();

                JsonNode usageNode = root.path("usage");
                long promptTokens = usageNode.path("prompt_tokens").asLong(0);
                long completionTokens = usageNode.path("completion_tokens").asLong(0);
                String returnedModel = root.path("model").asText(model);

                return LlmResponse.of(content, promptTokens, completionTokens, returnedModel);
            } catch (HttpClientErrorException.TooManyRequests | HttpServerErrorException e) {
                log.warn("LLM API rate limit or server error on attempt {}/{}: {}", attempt, maxRetries, e.getMessage());
                if (attempt == maxRetries) throw new RuntimeException("LLM request failed after retries: " + e.getMessage(), e);
                sleep(backoffMs);
                backoffMs *= 2;
            } catch (Exception e) {
                log.error("LLM request error: {}", e.getMessage());
                throw new RuntimeException("LLM call failed: " + e.getMessage(), e);
            }
        }
        throw new RuntimeException("LLM call failed without result");
    }

    private String extractJson(String text) {
        if (text == null) return "{}";
        String trimmed = text.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
