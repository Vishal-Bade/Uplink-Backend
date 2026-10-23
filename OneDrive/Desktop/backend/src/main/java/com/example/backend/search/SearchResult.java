package com.example.backend.search;

import com.example.backend.research.enums.SourceType;

public record SearchResult(
        String title,
        String url,
        String domain,
        String snippet,
        String authors,
        String publishedDate,
        Double score,
        SourceType sourceType
) {
    public static SearchResult ofWeb(String title, String url, String domain, String snippet) {
        return new SearchResult(title, url, domain, snippet, null, null, 1.0, SourceType.WEB);
    }

    public static SearchResult ofAcademic(String title, String url, String domain, String snippet, String authors, String publishedDate) {
        return new SearchResult(title, url, domain, snippet, authors, publishedDate, 1.0, SourceType.ACADEMIC);
    }
}
