package com.example.backend.research.entity;

import com.example.backend.research.enums.SourceType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sources", indexes = {
    @Index(name = "idx_sources_url", columnList = "url"),
    @Index(name = "idx_sources_domain", columnList = "domain")
})
public class Source {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(columnDefinition = "TEXT", nullable = false, unique = true)
    private String url;

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(length = 255)
    private String domain;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private SourceType sourceType;

    @Column(columnDefinition = "TEXT")
    private String authors;

    @Column(name = "published_date", length = 50)
    private String publishedDate;

    @Column(columnDefinition = "TEXT")
    private String snippet;

    @Column(name = "raw_content", columnDefinition = "TEXT")
    private String rawContent;

    @Column(name = "cleaned_content", columnDefinition = "TEXT")
    private String cleanedContent;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Source() {}

    public Source(String url, String title, String domain, SourceType sourceType, String snippet) {
        this.url = url;
        this.title = title;
        this.domain = domain;
        this.sourceType = sourceType;
        this.snippet = snippet;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.sourceType == null) {
            this.sourceType = SourceType.WEB;
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public SourceType getSourceType() { return sourceType; }
    public void setSourceType(SourceType sourceType) { this.sourceType = sourceType; }

    public String getAuthors() { return authors; }
    public void setAuthors(String authors) { this.authors = authors; }

    public String getPublishedDate() { return publishedDate; }
    public void setPublishedDate(String publishedDate) { this.publishedDate = publishedDate; }

    public String getSnippet() { return snippet; }
    public void setSnippet(String snippet) { this.snippet = snippet; }

    public String getRawContent() { return rawContent; }
    public void setRawContent(String rawContent) { this.rawContent = rawContent; }

    public String getCleanedContent() { return cleanedContent; }
    public void setCleanedContent(String cleanedContent) { this.cleanedContent = cleanedContent; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
