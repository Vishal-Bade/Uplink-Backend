package com.example.backend.research.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "run_sources", uniqueConstraints = {
    @UniqueConstraint(name = "uq_run_source", columnNames = {"run_id", "source_id"})
}, indexes = {
    @Index(name = "idx_run_sources_run_id", columnList = "run_id"),
    @Index(name = "idx_run_sources_source_id", columnList = "source_id")
})
public class RunSource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private ResearchRun run;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    @Column(name = "relevance_score")
    private Double relevanceScore;

    @Column(name = "relevance_explanation", columnDefinition = "TEXT")
    private String relevanceExplanation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public RunSource() {}

    public RunSource(ResearchRun run, Source source, Double relevanceScore, String relevanceExplanation) {
        this.run = run;
        this.source = source;
        this.relevanceScore = relevanceScore;
        this.relevanceExplanation = relevanceExplanation;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ResearchRun getRun() { return run; }
    public void setRun(ResearchRun run) { this.run = run; }

    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }

    public Double getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(Double relevanceScore) { this.relevanceScore = relevanceScore; }

    public String getRelevanceExplanation() { return relevanceExplanation; }
    public void setRelevanceExplanation(String relevanceExplanation) { this.relevanceExplanation = relevanceExplanation; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
