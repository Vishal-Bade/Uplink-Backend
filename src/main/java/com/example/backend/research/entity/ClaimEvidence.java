package com.example.backend.research.entity;

import com.example.backend.research.enums.EvidenceType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "claim_evidence", indexes = {
    @Index(name = "idx_claim_evidence_claim_id", columnList = "claim_id"),
    @Index(name = "idx_claim_evidence_source_id", columnList = "source_id")
})
public class ClaimEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chunk_id")
    private DocumentChunk chunk;

    @Column(name = "evidence_text", columnDefinition = "TEXT", nullable = false)
    private String evidenceText;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false, length = 30)
    private EvidenceType evidenceType = EvidenceType.SUPPORTING;

    @Column(name = "section_or_page", length = 100)
    private String sectionOrPage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ClaimEvidence() {}

    public ClaimEvidence(Claim claim, Source source, DocumentChunk chunk, String evidenceText, EvidenceType evidenceType, String sectionOrPage) {
        this.claim = claim;
        this.source = source;
        this.chunk = chunk;
        this.evidenceText = evidenceText;
        this.evidenceType = evidenceType;
        this.sectionOrPage = sectionOrPage;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.evidenceType == null) {
            this.evidenceType = EvidenceType.SUPPORTING;
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Claim getClaim() { return claim; }
    public void setClaim(Claim claim) { this.claim = claim; }

    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }

    public DocumentChunk getChunk() { return chunk; }
    public void setChunk(DocumentChunk chunk) { this.chunk = chunk; }

    public String getEvidenceText() { return evidenceText; }
    public void setEvidenceText(String evidenceText) { this.evidenceText = evidenceText; }

    public EvidenceType getEvidenceType() { return evidenceType; }
    public void setEvidenceType(EvidenceType evidenceType) { this.evidenceType = evidenceType; }

    public String getSectionOrPage() { return sectionOrPage; }
    public void setSectionOrPage(String sectionOrPage) { this.sectionOrPage = sectionOrPage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
