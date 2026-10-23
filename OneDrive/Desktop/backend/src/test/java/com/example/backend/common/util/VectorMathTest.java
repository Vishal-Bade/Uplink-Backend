package com.example.backend.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VectorMathTest {

    @Test
    void testCosineSimilarityIdenticalVectors() {
        float[] v1 = new float[]{1.0f, 2.0f, 3.0f};
        float[] v2 = new float[]{1.0f, 2.0f, 3.0f};
        double similarity = VectorMath.cosineSimilarity(v1, v2);
        assertEquals(1.0, similarity, 0.0001);
    }

    @Test
    void testCosineSimilarityOrthogonalVectors() {
        float[] v1 = new float[]{1.0f, 0.0f};
        float[] v2 = new float[]{0.0f, 1.0f};
        double similarity = VectorMath.cosineSimilarity(v1, v2);
        assertEquals(0.0, similarity, 0.0001);
    }

    @Test
    void testSerializeDeserialize() {
        float[] original = new float[]{0.15f, -0.42f, 0.99f};
        String serialized = VectorMath.serialize(original);
        float[] deserialized = VectorMath.deserialize(serialized);

        assertEquals(original.length, deserialized.length);
        for (int i = 0; i < original.length; i++) {
            assertEquals(original[i], deserialized[i], 0.0001f);
        }
    }

    @Test
    void testPseudoEmbeddingDeterministic() {
        float[] e1 = VectorMath.generatePseudoEmbedding("Semiconductor Policy 2024", 128);
        float[] e2 = VectorMath.generatePseudoEmbedding("Semiconductor Policy 2024", 128);
        float[] e3 = VectorMath.generatePseudoEmbedding("Cooking recipe for pizza", 128);

        assertEquals(128, e1.length);
        assertEquals(1.0, VectorMath.cosineSimilarity(e1, e2), 0.0001);
        assertTrue(VectorMath.cosineSimilarity(e1, e3) < 0.95);
    }
}
