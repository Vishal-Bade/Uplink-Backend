package com.example.backend.research.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "citations", indexes = {
    @Index(name = "idx_citations_report_id", columnList = "report_id"),
    @Index(name = "idx_citations_source_id", columnList = "source_id")
})
public class Citation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    @Column(name = "citation_number", nullable = false)
    private int citationNumber;

    @Column(name = "citation_key", nullable = false, length = 50)
    private String citationKey;

    @Column(name = "formatted_ieee", columnDefinition = "TEXT", nullable = false)
    private String formattedIeee;

    @Column(name = "formatted_apa", columnDefinition = "TEXT", nullable = false)
    private String formattedApa;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Citation() {}

    public Citation(Report report, Source source, int citationNumber, String citationKey, String formattedIeee, String formattedApa) {
        this.report = report;
        this.source = source;
        this.citationNumber = citationNumber;
        this.citationKey = citationKey;
        this.formattedIeee = formattedIeee;
        this.formattedApa = formattedApa;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Report getReport() { return report; }
    public void setReport(Report report) { this.report = report; }

    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }

    public int getCitationNumber() { return citationNumber; }
    public void setCitationNumber(int citationNumber) { this.citationNumber = citationNumber; }

    public String getCitationKey() { return citationKey; }
    public void setCitationKey(String citationKey) { this.citationKey = citationKey; }

    public String getFormattedIeee() { return formattedIeee; }
    public void setFormattedIeee(String formattedIeee) { this.formattedIeee = formattedIeee; }

    public String getFormattedApa() { return formattedApa; }
    public void setFormattedApa(String formattedApa) { this.formattedApa = formattedApa; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
