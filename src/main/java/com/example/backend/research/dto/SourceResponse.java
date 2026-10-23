package com.example.backend.research.dto;

import com.example.backend.research.entity.RunSource;
import com.example.backend.research.entity.Source;
import com.example.backend.research.enums.SourceType;

import java.time.LocalDateTime;
import java.util.UUID;

public record SourceResponse(
        UUID id,
        String url,
        String title,
        String domain,
        SourceType sourceType,
        String authors,
        String publishedDate,
        String snippet,
        Double relevanceScore,
        String relevanceExplanation,
        LocalDateTime createdAt
) {
    public static SourceResponse from(RunSource rs) {
        Source s = rs.getSource();
        return new SourceResponse(
                s.getId(),
                s.getUrl(),
                s.getTitle(),
                s.getDomain(),
                s.getSourceType(),
                s.getAuthors(),
                s.getPublishedDate(),
                s.getSnippet(),
                rs.getRelevanceScore(),
                rs.getRelevanceExplanation(),
                s.getCreatedAt()
        );
    }

    public static SourceResponse from(Source s) {
        return new SourceResponse(
                s.getId(),
                s.getUrl(),
                s.getTitle(),
                s.getDomain(),
                s.getSourceType(),
                s.getAuthors(),
                s.getPublishedDate(),
                s.getSnippet(),
                1.0,
                null,
                s.getCreatedAt()
        );
    }
}
