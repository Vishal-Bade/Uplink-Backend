package com.example.backend.search;

import com.example.backend.research.enums.SourceType;

import java.util.ArrayList;
import java.util.List;

public class MockSearchProvider implements SearchProvider {

    @Override
    public List<SearchResult> search(String query, int maxResults) {
        List<SearchResult> results = new ArrayList<>();
        String cleanQuery = query != null ? query.trim() : "technology";
        String slug = cleanQuery.toLowerCase().replaceAll("[^a-z0-9]+", "-");

        results.add(new SearchResult(
                "National Strategic Initiative and Policy Framework for " + cleanQuery,
                "https://gov.research.org/initiatives/" + slug,
                "gov.research.org",
                "Official policy report examining capital expenditure incentives, regulatory standards, and institutional support for " + cleanQuery + ".",
                "Ministry of Technology",
                "2024",
                0.95,
                SourceType.WEB
        ));

        results.add(new SearchResult(
                "Global Industry Consortium Analysis: Market Growth and Investment in " + cleanQuery,
                "https://industry-insights.com/reports/" + slug + "-market-analysis",
                "industry-insights.com",
                "Comprehensive commercial review detailing market capitalization, venture capital flows, infrastructure construction, and enterprise adoption in " + cleanQuery + ".",
                "Enterprise Research Group",
                "2024",
                0.90,
                SourceType.WEB
        ));

        results.add(new SearchResult(
                "Technical Ecosystem Vulnerabilities and Supply Chain Architecture: An Empirical Study of " + cleanQuery,
                "https://academic-review.edu/papers/" + slug + "-supply-chain",
                "academic-review.edu",
                "Peer-reviewed academic assessment identifying critical raw material dependencies, yield stabilization challenges, and international supply chain choke points.",
                "Dr. A. Sharma, Dr. R. Chen et al.",
                "2023",
                0.88,
                SourceType.ACADEMIC
        ));

        results.add(new SearchResult(
                "Next-Generation Roadmap and Economic Impact Forecast for " + cleanQuery + " (2025-2035)",
                "https://tech-future.org/briefs/" + slug + "-outlook-2035",
                "tech-future.org",
                "Forward-looking strategic forecast mapping technological milestones, productivity dividends, and emerging market opportunities across diverse sectors.",
                "Technology Futures Institute",
                "2024",
                0.85,
                SourceType.WEB
        ));

        return results.subList(0, Math.min(results.size(), maxResults));
    }

    @Override
    public String getProviderName() {
        return "Mock-Search-Engine";
    }
}
