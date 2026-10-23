package com.example.backend.search;

import com.example.backend.common.util.UrlValidator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DuckDuckGoHtmlSearchProvider implements SearchProvider {

    private static final Logger log = LoggerFactory.getLogger(DuckDuckGoHtmlSearchProvider.class);
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36";

    @Override
    public List<SearchResult> search(String query, int maxResults) {
        List<SearchResult> results = new ArrayList<>();
        if (query == null || query.isBlank()) {
            return results;
        }

        try {
            String searchUrl = "https://html.duckduckgo.com/html/?q=" + java.net.URLEncoder.encode(query, StandardCharsets.UTF_8);
            Document doc = Jsoup.connect(searchUrl)
                    .userAgent(USER_AGENT)
                    .timeout(10000)
                    .referrer("https://duckduckgo.com/")
                    .get();

            Elements resultElements = doc.select(".results .result");
            for (Element element : resultElements) {
                if (results.size() >= maxResults) {
                    break;
                }

                Element titleLink = element.selectFirst(".result__title a");
                Element snippetElement = element.selectFirst(".result__snippet");

                if (titleLink == null) continue;

                String rawHref = titleLink.attr("href");
                String actualUrl = extractActualUrl(rawHref);
                if (actualUrl == null || !UrlValidator.isSafeUrl(actualUrl)) {
                    continue;
                }

                String title = titleLink.text().trim();
                String snippet = snippetElement != null ? snippetElement.text().trim() : "";
                String domain = extractDomain(actualUrl);

                results.add(SearchResult.ofWeb(title, actualUrl, domain, snippet));
            }
            log.info("DuckDuckGo search for '{}' retrieved {} results", query, results.size());
        } catch (Exception e) {
            log.warn("DuckDuckGo search failed for '{}': {}", query, e.getMessage());
        }

        return results;
    }

    @Override
    public String getProviderName() {
        return "DuckDuckGo-HTML";
    }

    private String extractActualUrl(String href) {
        if (href == null || href.isBlank()) return null;
        if (href.startsWith("//duckduckgo.com/l/?uddg=")) {
            href = "https:" + href;
        }
        if (href.contains("uddg=")) {
            try {
                int start = href.indexOf("uddg=") + 5;
                int end = href.indexOf("&", start);
                String encoded = end != -1 ? href.substring(start, end) : href.substring(start);
                return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
            } catch (Exception e) {
                return null;
            }
        }
        if (href.startsWith("http://") || href.startsWith("https://")) {
            return href;
        }
        return null;
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
