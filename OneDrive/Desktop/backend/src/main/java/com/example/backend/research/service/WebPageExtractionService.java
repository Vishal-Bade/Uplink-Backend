package com.example.backend.research.service;

import com.example.backend.common.util.UrlValidator;
import com.example.backend.research.entity.Source;
import com.example.backend.research.repository.SourceRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class WebPageExtractionService {

    private static final Logger log = LoggerFactory.getLogger(WebPageExtractionService.class);
    private static final int MAX_BODY_BYTES = 2 * 1024 * 1024; // 2 MB limit
    private static final int TIMEOUT_MS = 10000;
    private static final String USER_AGENT = "AutonomousResearchWorkspace/1.0 (+https://example.com/bot)";

    private final SourceRepository sourceRepository;

    public WebPageExtractionService(SourceRepository sourceRepository) {
        this.sourceRepository = sourceRepository;
    }

    @Transactional
    public Source extractAndCleanContent(Source source) {
        if (source.getCleanedContent() != null && !source.getCleanedContent().isBlank()) {
            return source; // Already extracted
        }

        String url = source.getUrl();
        if (!UrlValidator.isSafeUrl(url)) {
            log.warn("Blocked extraction for unsafe URL: {}", url);
            useFallbackSnippet(source);
            return sourceRepository.save(source);
        }

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .maxBodySize(MAX_BODY_BYTES)
                    .followRedirects(true)
                    .get();

            // Extract metadata if missing
            if (source.getTitle() == null || source.getTitle().equals(source.getUrl())) {
                String pageTitle = doc.title();
                if (!pageTitle.isBlank()) {
                    source.setTitle(pageTitle.trim());
                }
            }

            // Remove non-content elements
            doc.select("script, style, noscript, nav, header, footer, aside, .sidebar, .advertisement, .ad, form, svg").remove();

            // Extract main article or body
            Element mainElement = doc.selectFirst("article, main, [role=main], .content, #content, .post-content");
            String rawHtml = mainElement != null ? mainElement.html() : doc.body().html();
            String cleanText = Jsoup.clean(rawHtml, Safelist.none());

            // Normalize whitespace
            cleanText = cleanText.replaceAll("\\s+", " ").trim();

            if (cleanText.length() > 50) {
                source.setRawContent(rawHtml.length() > 50000 ? rawHtml.substring(0, 50000) : rawHtml);
                source.setCleanedContent(cleanText);
                source.setContentHash(computeSha256(cleanText));
                log.info("Extracted {} characters of clean text from {}", cleanText.length(), url);
            } else {
                useFallbackSnippet(source);
            }
        } catch (Exception e) {
            log.warn("Failed to extract content from {}: {}. Falling back to snippet.", url, e.getMessage());
            useFallbackSnippet(source);
        }

        return sourceRepository.save(source);
    }

    private void useFallbackSnippet(Source source) {
        String snippet = source.getSnippet();
        if (snippet != null && !snippet.isBlank()) {
            source.setCleanedContent(snippet);
            source.setContentHash(computeSha256(snippet));
        } else {
            String fallback = "Source content from " + source.getTitle() + " at " + source.getUrl();
            source.setCleanedContent(fallback);
            source.setContentHash(computeSha256(fallback));
        }
    }

    private String computeSha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return Integer.toHexString(text.hashCode());
        }
    }
}
