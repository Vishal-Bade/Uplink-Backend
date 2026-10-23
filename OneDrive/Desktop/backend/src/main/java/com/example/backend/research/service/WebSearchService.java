package com.example.backend.research.service;

import com.example.backend.research.entity.ResearchRun;
import com.example.backend.research.entity.RunSource;
import com.example.backend.research.entity.Source;
import com.example.backend.research.enums.SourceType;
import com.example.backend.research.repository.RunSourceRepository;
import com.example.backend.research.repository.SourceRepository;
import com.example.backend.search.SearchProvider;
import com.example.backend.search.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class WebSearchService {

    private static final Logger log = LoggerFactory.getLogger(WebSearchService.class);
    private final SearchProvider webSearchProvider;
    private final SourceRepository sourceRepository;
    private final RunSourceRepository runSourceRepository;

    public WebSearchService(
            @Qualifier("webSearchProvider") SearchProvider webSearchProvider,
            SourceRepository sourceRepository,
            RunSourceRepository runSourceRepository) {
        this.webSearchProvider = webSearchProvider;
        this.sourceRepository = sourceRepository;
        this.runSourceRepository = runSourceRepository;
    }

    public List<Source> searchAndCollectSources(ResearchRun run, List<String> queries, int maxSources) {
        log.info("Searching web sources for run {} with {} queries, limit: {}", run.getId(), queries.size(), maxSources);
        Set<String> processedUrls = new HashSet<>();
        List<Source> collectedSources = new ArrayList<>();

        for (String query : queries) {
            if (collectedSources.size() >= maxSources) {
                break;
            }

            try {
                List<SearchResult> searchResults = webSearchProvider.search(query, 5);
                for (SearchResult result : searchResults) {
                    if (collectedSources.size() >= maxSources) {
                        break;
                    }

                    String normalizedUrl = normalizeUrl(result.url());
                    if (normalizedUrl == null || processedUrls.contains(normalizedUrl)) {
                        continue;
                    }
                    processedUrls.add(normalizedUrl);

                    Source source = findOrCreateSource(result, normalizedUrl);
                    linkSourceToRun(run, source, result.score(), "Retrieved via query: " + query);
                    collectedSources.add(source);
                }
            } catch (Exception e) {
                log.warn("Search execution failed for query '{}': {}", query, e.getMessage());
            }
        }

        run.setTotalSourcesFound(run.getTotalSourcesFound() + collectedSources.size());
        log.info("Collected and persisted {} unique sources for run {}", collectedSources.size(), run.getId());
        return collectedSources;
    }

    private Source findOrCreateSource(SearchResult result, String normalizedUrl) {
        return sourceRepository.findByUrl(normalizedUrl).orElseGet(() -> {
            Source newSource = new Source();
            newSource.setUrl(normalizedUrl);
            newSource.setTitle(result.title() != null ? result.title() : normalizedUrl);
            newSource.setDomain(result.domain());
            newSource.setSourceType(result.sourceType() != null ? result.sourceType() : SourceType.WEB);
            newSource.setAuthors(result.authors());
            newSource.setPublishedDate(result.publishedDate());
            newSource.setSnippet(result.snippet());
            return sourceRepository.save(newSource);
        });
    }

    private void linkSourceToRun(ResearchRun run, Source source, Double score, String explanation) {
        if (!runSourceRepository.findByRunIdAndSourceId(run.getId(), source.getId()).isPresent()) {
            RunSource runSource = new RunSource(run, source, score != null ? score : 1.0, explanation);
            runSourceRepository.save(runSource);
        }
    }

    private String normalizeUrl(String url) {
        if (url == null || url.isBlank()) return null;
        String trimmed = url.trim();
        // Remove trailing slash
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        // Strip common tracking query params
        int questionIdx = trimmed.indexOf("?");
        if (questionIdx != -1) {
            String base = trimmed.substring(0, questionIdx);
            String query = trimmed.substring(questionIdx + 1);
            String[] params = query.split("&");
            StringBuilder cleanQuery = new StringBuilder();
            for (String param : params) {
                String pLower = param.toLowerCase();
                if (!pLower.startsWith("utm_") && !pLower.startsWith("fbclid") && !pLower.startsWith("gclid")) {
                    if (cleanQuery.length() > 0) cleanQuery.append("&");
                    cleanQuery.append(param);
                }
            }
            trimmed = cleanQuery.length() > 0 ? base + "?" + cleanQuery : base;
        }
        return trimmed;
    }
}
