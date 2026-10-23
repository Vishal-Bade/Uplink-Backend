package com.example.backend.common.util;

import java.util.Arrays;

public final class VectorMath {

    private VectorMath() {}

    public static double cosineSimilarity(float[] v1, float[] v2) {
        if (v1 == null || v2 == null || v1.length != v2.length || v1.length == 0) {
            return 0.0;
        }

        double dot = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < v1.length; i++) {
            dot += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }

        if (norm1 <= 0.0 || norm2 <= 0.0) {
            return 0.0;
        }

        return dot / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    public static String serialize(float[] vector) {
        if (vector == null) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    public static float[] deserialize(String str) {
        if (str == null || str.isBlank()) {
            return new float[0];
        }
        String cleaned = str.trim();
        if (cleaned.startsWith("[")) cleaned = cleaned.substring(1);
        if (cleaned.endsWith("]")) cleaned = cleaned.substring(0, cleaned.length() - 1);
        if (cleaned.isBlank()) return new float[0];

        String[] parts = cleaned.split(",");
        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                vector[i] = Float.parseFloat(parts[i].trim());
            } catch (NumberFormatException e) {
                vector[i] = 0.0f;
            }
        }
        return vector;
    }

    /**
     * Generates a deterministic pseudo-semantic vector from text (useful for offline mock embeddings).
     */
    public static float[] generatePseudoEmbedding(String text, int dimension) {
        float[] vector = new float[dimension];
        if (text == null || text.isBlank()) {
            return vector;
        }

        long hash = 1125899906842597L; // FNV offset basis
        for (int i = 0; i < text.length(); i++) {
            hash ^= text.charAt(i);
            hash *= 1099511628211L; // FNV prime
            int dimIdx = Math.abs((int) (hash % dimension));
            vector[dimIdx] += 1.0f;
        }

        // Normalize
        double norm = 0.0;
        for (float v : vector) {
            norm += v * v;
        }
        if (norm > 0.0) {
            float sqrtNorm = (float) Math.sqrt(norm);
            for (int i = 0; i < dimension; i++) {
                vector[i] /= sqrtNorm;
            }
        }

        return vector;
    }
}
