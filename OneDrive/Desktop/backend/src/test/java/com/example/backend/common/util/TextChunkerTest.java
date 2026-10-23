package com.example.backend.common.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TextChunkerTest {

    @Test
    void testSmallTextSingleChunk() {
        TextChunker chunker = new TextChunker(500, 50);
        String text = "India is advancing its semiconductor manufacturing ecosystem with major government subsidies.";
        List<TextChunker.Chunk> chunks = chunker.chunk(text);

        assertEquals(1, chunks.size());
        assertEquals(0, chunks.get(0).index());
        assertEquals(text, chunks.get(0).content());
        assertTrue(chunks.get(0).tokenCount() > 0);
        assertEquals(0, chunks.get(0).startOffset());
        assertEquals(text.length(), chunks.get(0).endOffset());
    }

    @Test
    void testLongTextChunkingWithOverlap() {
        TextChunker chunker = new TextChunker(100, 20);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            sb.append("Sentence ").append(i).append(" discussing semiconductor capital investments. ");
        }
        String text = sb.toString();
        List<TextChunker.Chunk> chunks = chunker.chunk(text);

        assertTrue(chunks.size() > 1);
        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(i, chunks.get(i).index());
            assertFalse(chunks.get(i).content().isBlank());
            assertTrue(chunks.get(i).endOffset() > chunks.get(i).startOffset());
        }
    }

    @Test
    void testEmptyAndNullHandling() {
        TextChunker chunker = new TextChunker();
        assertTrue(chunker.chunk(null).isEmpty());
        assertTrue(chunker.chunk("   ").isEmpty());
    }
}
