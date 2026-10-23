package com.example.backend.common.util;

import java.util.ArrayList;
import java.util.List;

public class TextChunker {

    public record Chunk(
            int index,
            String content,
            int tokenCount,
            int startOffset,
            int endOffset
    ) {}

    private final int targetChunkChars;
    private final int overlapChars;

    public TextChunker() {
        this(1200, 200); // ~300 tokens target, ~50 tokens overlap
    }

    public TextChunker(int targetChunkChars, int overlapChars) {
        this.targetChunkChars = Math.max(200, targetChunkChars);
        this.overlapChars = Math.max(0, Math.min(overlapChars, targetChunkChars / 2));
    }

    public List<Chunk> chunk(String text) {
        List<Chunk> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        String cleaned = text.trim();
        int length = cleaned.length();
        if (length <= targetChunkChars) {
            chunks.add(new Chunk(0, cleaned, estimateTokens(cleaned), 0, length));
            return chunks;
        }

        int start = 0;
        int chunkIndex = 0;

        while (start < length) {
            int end = Math.min(start + targetChunkChars, length);

            // Find a natural break boundary (paragraph break, period, newline, or space)
            if (end < length) {
                int boundary = findBreakBoundary(cleaned, start, end);
                if (boundary > start + (targetChunkChars / 2)) {
                    end = boundary;
                }
            }

            String chunkText = cleaned.substring(start, end).trim();
            if (!chunkText.isEmpty()) {
                chunks.add(new Chunk(
                        chunkIndex++,
                        chunkText,
                        estimateTokens(chunkText),
                        start,
                        end
                ));
            }

            if (end >= length) {
                break;
            }

            // Move start forward, respecting overlap
            start = Math.max(start + 1, end - overlapChars);
        }

        return chunks;
    }

    private int findBreakBoundary(String text, int start, int end) {
        // Look backwards from end for paragraph break, period, or question/exclamation mark
        for (int i = end; i > start + (targetChunkChars / 2); i--) {
            if (i < text.length() - 1 && text.charAt(i) == '\n' && text.charAt(i - 1) == '\n') {
                return i;
            }
        }
        for (int i = end; i > start + (targetChunkChars / 2); i--) {
            char c = text.charAt(i - 1);
            if ((c == '.' || c == '?' || c == '!') && (i == text.length() || Character.isWhitespace(text.charAt(i)))) {
                return i;
            }
        }
        for (int i = end; i > start + (targetChunkChars / 2); i--) {
            if (Character.isWhitespace(text.charAt(i - 1))) {
                return i;
            }
        }
        return end;
    }

    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // General rule of thumb: ~4 characters per token in English
        return Math.max(1, text.length() / 4);
    }
}
