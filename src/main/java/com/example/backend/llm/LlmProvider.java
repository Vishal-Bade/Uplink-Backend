package com.example.backend.llm;

public interface LlmProvider {
    LlmResponse<String> generateText(String prompt, String systemInstruction);
    <T> LlmResponse<T> generateStructured(String prompt, String systemInstruction, Class<T> responseClass);
    float[] generateEmbedding(String text);
    String getProviderName();
}
