package com.example.backend.search;

import com.example.backend.common.util.UrlValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TavilySearchProvider implements SearchProvider {

    private static final Logger log = LoggerFactory.getLogger(TavilySearchProvider.class);
    private final String apiKey;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public TavilySearchProvider(String apiKey, ObjectMapper objectMapper) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(15));
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl("https://api.tavily.com")
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public List<SearchResult> search(String query, int maxResults) {
        List<SearchResult> results = new ArrayList<>();
        if (query == null || query.isBlank() || apiKey.isBlank()) return results;

        try {
            Map<String, Object> body = Map.of(
                    "api_key", apiKey,
                    "query", query,
                    "search_depth", "basic",
                    "max_results", Math.min(maxResults, 10)
            );

            String response = restClient.post()
                    .uri("/search")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode resultsArray = root.path("results");

            if (resultsArray.isArray()) {
                for (JsonNode item : resultsArray) {
                    String title = item.path("title").asText("");
                    String url = item.path("url").asText("");
                    String content = item.path("content").asText("");
                    Double score = item.path("score").asDouble(1.0);

                    if (url.isBlank() || !UrlValidator.isSafeUrl(url)) {
                        continue;
                    }

                    String domain = extractDomain(url);
                    results.add(new SearchResult(title, url, domain, content, null, null, score, com.example.backend.research.enums.SourceType.WEB));
                }
            }
            log.info("Tavily search for '{}' retrieved {} results", query, results.size());
        } catch (Exception e) {
            log.warn("Tavily search failed for '{}': {}", query, e.getMessage());
        }

        return results;
    }

    @Override
    public String getProviderName() {
        return "Tavily-Search";
    }

    private String extractDomain(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            return host != null ? host.replaceAll("^www\\.", "") : "";
        } catch (Exception e) {
            return "";
        }
    }
}
