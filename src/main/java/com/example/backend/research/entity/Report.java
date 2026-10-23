package com.example.backend.research.entity;

import com.example.backend.project.entity.Project;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reports", indexes = {
    @Index(name = "idx_reports_project_id", columnList = "project_id"),
    @Index(name = "idx_reports_run_id", columnList = "run_id")
})
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false, unique = true)
    private ResearchRun run;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String title;

    @Column(name = "executive_summary", columnDefinition = "TEXT")
    private String executiveSummary;

    @Column(name = "objectives_scope", columnDefinition = "TEXT")
    private String objectivesScope;

    @Column(columnDefinition = "TEXT")
    private String methodology;

    @Column(name = "key_findings", columnDefinition = "TEXT")
    private String keyFindings;

    @Column(name = "detailed_analysis", columnDefinition = "TEXT")
    private String detailedAnalysis;

    @Column(name = "contradictions_limitations", columnDefinition = "TEXT")
    private String contradictionsLimitations;

    @Column(columnDefinition = "TEXT")
    private String conclusions;

    @Column(name = "citations_formatted", columnDefinition = "TEXT")
    private String citationsFormatted;

    @Column(nullable = false)
    private int version = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Report() {}

    public Report(ResearchRun run, Project project, String title) {
        this.run = run;
        this.project = project;
        this.title = title;
        this.version = 1;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ResearchRun getRun() { return run; }
    public void setRun(ResearchRun run) { this.run = run; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getExecutiveSummary() { return executiveSummary; }
    public void setExecutiveSummary(String executiveSummary) { this.executiveSummary = executiveSummary; }

    public String getObjectivesScope() { return objectivesScope; }
    public void setObjectivesScope(String objectivesScope) { this.objectivesScope = objectivesScope; }

    public String getMethodology() { return methodology; }
    public void setMethodology(String methodology) { this.methodology = methodology; }

    public String getKeyFindings() { return keyFindings; }
    public void setKeyFindings(String keyFindings) { this.keyFindings = keyFindings; }

    public String getDetailedAnalysis() { return detailedAnalysis; }
    public void setDetailedAnalysis(String detailedAnalysis) { this.detailedAnalysis = detailedAnalysis; }

    public String getContradictionsLimitations() { return contradictionsLimitations; }
    public void setContradictionsLimitations(String contradictionsLimitations) { this.contradictionsLimitations = contradictionsLimitations; }

    public String getConclusions() { return conclusions; }
    public void setConclusions(String conclusions) { this.conclusions = conclusions; }

    public String getCitationsFormatted() { return citationsFormatted; }
    public void setCitationsFormatted(String citationsFormatted) { this.citationsFormatted = citationsFormatted; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
