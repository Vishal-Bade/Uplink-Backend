package com.example.backend.search;

import com.example.backend.common.util.UrlValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class SemanticScholarSearchProvider implements SearchProvider {

    private static final Logger log = LoggerFactory.getLogger(SemanticScholarSearchProvider.class);
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public SemanticScholarSearchProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(15));
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl("https://api.semanticscholar.org")
                .build();
    }

    @Override
    public List<SearchResult> search(String query, int maxResults) {
        List<SearchResult> results = new ArrayList<>();
        if (query == null || query.isBlank()) return results;

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "/graph/v1/paper/search?query=" + encodedQuery + "&limit=" + Math.min(maxResults, 10) + "&fields=title,url,authors,year,abstract";

            String response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode dataArray = root.path("data");

            if (dataArray.isArray()) {
                for (JsonNode paper : dataArray) {
                    String title = paper.path("title").asText("");
                    String paperUrl = paper.path("url").asText("");
                    if (paperUrl.isBlank()) {
                        String paperId = paper.path("paperId").asText("");
                        if (!paperId.isBlank()) {
                            paperUrl = "https://www.semanticscholar.org/paper/" + paperId;
                        }
                    }

                    if (paperUrl.isBlank() || !UrlValidator.isSafeUrl(paperUrl)) {
                        continue;
                    }

                    String snippet = paper.path("abstract").asText("");
                    if (snippet.isBlank()) {
                        snippet = title;
                    }

                    StringBuilder authorsBuilder = new StringBuilder();
                    JsonNode authorsNode = paper.path("authors");
                    if (authorsNode.isArray()) {
                        for (int i = 0; i < authorsNode.size(); i++) {
                            if (i > 0) authorsBuilder.append(", ");
                            authorsBuilder.append(authorsNode.get(i).path("name").asText(""));
                            if (i >= 3) {
                                authorsBuilder.append(" et al.");
                                break;
                            }
                        }
                    }

                    String year = paper.path("year").asText("");
                    String domain = extractDomain(paperUrl);

                    results.add(SearchResult.ofAcademic(
                            title,
                            paperUrl,
                            domain,
                            snippet,
                            authorsBuilder.toString(),
                            year
                    ));
                }
            }
            log.info("SemanticScholar search for '{}' retrieved {} papers", query, results.size());
        } catch (Exception e) {
            log.warn("SemanticScholar academic search failed for '{}': {}", query, e.getMessage());
        }

        return results;
    }

    @Override
    public String getProviderName() {
        return "SemanticScholar-Academic";
    }

    private String extractDomain(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            return host != null ? host.replaceAll("^www\\.", "") : "semanticscholar.org";
        } catch (Exception e) {
            return "semanticscholar.org";
        }
    }
}
