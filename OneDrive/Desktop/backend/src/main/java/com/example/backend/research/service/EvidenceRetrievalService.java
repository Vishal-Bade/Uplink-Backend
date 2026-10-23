package com.example.backend.research.service;

import com.example.backend.common.util.VectorMath;
import com.example.backend.llm.LlmProvider;
import com.example.backend.research.entity.DocumentChunk;
import com.example.backend.research.repository.DocumentChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class EvidenceRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(EvidenceRetrievalService.class);
    private final DocumentChunkRepository documentChunkRepository;
    private final LlmProvider llmProvider;

    public record ScoredChunk(
            DocumentChunk chunk,
            double vectorScore,
            double keywordScore,
            double combinedScore
    ) {}

    public EvidenceRetrievalService(DocumentChunkRepository documentChunkRepository, LlmProvider llmProvider) {
        this.documentChunkRepository = documentChunkRepository;
        this.llmProvider = llmProvider;
    }

    public List<ScoredChunk> retrieveRelevantChunks(UUID runId, String query, int topK) {
        List<DocumentChunk> chunks = documentChunkRepository.findByRunIdWithSource(runId);
        if (chunks.isEmpty()) {
            return List.of();
        }

        float[] queryEmbedding;
        try {
            queryEmbedding = llmProvider.generateEmbedding(query);
        } catch (Exception e) {
            queryEmbedding = VectorMath.generatePseudoEmbedding(query, 1536);
        }

        Set<String> queryKeywords = extractKeywords(query);
        List<ScoredChunk> scoredList = new ArrayList<>();

        for (DocumentChunk chunk : chunks) {
            float[] chunkEmbedding = VectorMath.deserialize(chunk.getEmbedding());
            double vectorScore = VectorMath.cosineSimilarity(queryEmbedding, chunkEmbedding);
            double keywordScore = computeKeywordScore(chunk.getContent(), queryKeywords);

            // Hybrid score: 70% vector semantic similarity + 30% keyword overlap
            double combinedScore = (0.7 * Math.max(0.0, vectorScore)) + (0.3 * keywordScore);
            scoredList.add(new ScoredChunk(chunk, vectorScore, keywordScore, combinedScore));
        }

        // Sort descending by combined score
        scoredList.sort((a, b) -> Double.compare(b.combinedScore(), a.combinedScore()));

        int limit = Math.min(topK, scoredList.size());
        List<ScoredChunk> topChunks = scoredList.subList(0, limit);
        log.info("Retrieved {} relevant chunks for query: '{}' (top score: {})",
                topChunks.size(), query, topChunks.isEmpty() ? 0 : topChunks.get(0).combinedScore());
        return topChunks;
    }

    public String assembleContext(List<ScoredChunk> scoredChunks) {
        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (ScoredChunk sc : scoredChunks) {
            DocumentChunk c = sc.chunk();
            String sourceTitle = c.getSource() != null ? c.getSource().getTitle() : "Source";
            String sourceUrl = c.getSource() != null ? c.getSource().getUrl() : "";

            sb.append("[").append(index++).append("] Source: ").append(sourceTitle)
                    .append(" (").append(sourceUrl).append(")\n")
                    .append("Content: ").append(c.getContent().trim())
                    .append("\n\n");
        }
        return sb.toString();
    }

    private Set<String> extractKeywords(String text) {
        Set<String> keywords = new HashSet<>();
        if (text == null) return keywords;
        String[] tokens = text.toLowerCase().split("[^a-z0-9]+");
        for (String token : tokens) {
            if (token.length() > 3 && !isStopWord(token)) {
                keywords.add(token);
            }
        }
        return keywords;
    }

    private double computeKeywordScore(String content, Set<String> queryKeywords) {
        if (content == null || queryKeywords.isEmpty()) return 0.0;
        String lowerContent = content.toLowerCase();
        int matches = 0;
        for (String kw : queryKeywords) {
            if (lowerContent.contains(kw)) {
                matches++;
            }
        }
        return (double) matches / queryKeywords.size();
    }

    private boolean isStopWord(String word) {
        return Set.of("what", "when", "where", "which", "with", "from", "that", "this", "these", "those",
                "have", "been", "were", "about", "into", "over", "after", "before").contains(word);
    }
}
