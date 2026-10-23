package com.example.backend.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SearchConfig {

    private static final Logger log = LoggerFactory.getLogger(SearchConfig.class);

    @Value("${search.tavily-api-key:${TAVILY_API_KEY:}}")
    private String tavilyApiKey;

    @Value("${search.provider:auto}")
    private String configuredProvider;

    @Bean
    public SearchProvider webSearchProvider(ObjectMapper objectMapper) {
        if ("mock".equalsIgnoreCase(configuredProvider)) {
            log.info("Explicit mock search provider requested via configuration.");
            return new MockSearchProvider();
        }

        if (tavilyApiKey != null && !tavilyApiKey.isBlank()) {
            log.info("Active Tavily API key configured. Using TavilySearchProvider.");
            return new TavilySearchProvider(tavilyApiKey, objectMapper);
        }

        log.info("No Tavily API key detected. Using live DuckDuckGoHtmlSearchProvider with fallback.");
        return new CompositeSearchProvider(
                new DuckDuckGoHtmlSearchProvider(),
                new MockSearchProvider()
        );
    }

    @Bean
    public SearchProvider academicSearchProvider(ObjectMapper objectMapper) {
        if ("mock".equalsIgnoreCase(configuredProvider)) {
            return new MockSearchProvider();
        }
        return new CompositeSearchProvider(
                new SemanticScholarSearchProvider(objectMapper),
                new MockSearchProvider()
        );
    }

    /**
     * Composite provider that attempts primary live search, and falls back to secondary if empty or failed.
     */
    public static class CompositeSearchProvider implements SearchProvider {
        private final SearchProvider primary;
        private final SearchProvider fallback;

        public CompositeSearchProvider(SearchProvider primary, SearchProvider fallback) {
            this.primary = primary;
            this.fallback = fallback;
        }

        @Override
        public List<SearchResult> search(String query, int maxResults) {
            try {
                List<SearchResult> results = primary.search(query, maxResults);
                if (results != null && !results.isEmpty()) {
                    return results;
                }
            } catch (Exception e) {
                log.warn("Primary search provider {} failed: {}. Falling back to {}",
                        primary.getProviderName(), e.getMessage(), fallback.getProviderName());
            }
            return fallback.search(query, maxResults);
        }

        @Override
        public String getProviderName() {
            return primary.getProviderName() + " (with " + fallback.getProviderName() + " fallback)";
        }
    }
}
