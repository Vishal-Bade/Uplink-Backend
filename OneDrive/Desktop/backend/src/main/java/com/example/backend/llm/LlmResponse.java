package com.example.backend.llm;

public record LlmResponse<T>(
        T data,
        long promptTokens,
        long completionTokens,
        long totalTokens,
        String model
) {
    public static <T> LlmResponse<T> of(T data, long promptTokens, long completionTokens, String model) {
        return new LlmResponse<>(data, promptTokens, completionTokens, promptTokens + completionTokens, model);
    }

    public static <T> LlmResponse<T> of(T data) {
        return new LlmResponse<>(data, 0, 0, 0, "mock-model");
    }
}
