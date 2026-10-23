package com.example.backend.research.entity;

import com.example.backend.research.enums.ClaimStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "claims", indexes = {
    @Index(name = "idx_claims_run_id", columnList = "run_id"),
    @Index(name = "idx_claims_status", columnList = "verification_status")
})
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private ResearchRun run;

    @Column(name = "claim_text", columnDefinition = "TEXT", nullable = false)
    private String claimText;

    @Column(length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private ClaimStatus verificationStatus = ClaimStatus.UNVERIFIED;

    @Column(name = "verification_explanation", columnDefinition = "TEXT")
    private String verificationExplanation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Claim() {}

    public Claim(ResearchRun run, String claimText, String category) {
        this.run = run;
        this.claimText = claimText;
        this.category = category;
        this.verificationStatus = ClaimStatus.UNVERIFIED;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.verificationStatus == null) {
            this.verificationStatus = ClaimStatus.UNVERIFIED;
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ResearchRun getRun() { return run; }
    public void setRun(ResearchRun run) { this.run = run; }

    public String getClaimText() { return claimText; }
    public void setClaimText(String claimText) { this.claimText = claimText; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public ClaimStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(ClaimStatus verificationStatus) { this.verificationStatus = verificationStatus; }

    public String getVerificationExplanation() { return verificationExplanation; }
    public void setVerificationExplanation(String verificationExplanation) { this.verificationExplanation = verificationExplanation; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
