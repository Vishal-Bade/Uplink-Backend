package com.example.backend.research.entity;

import com.example.backend.project.entity.Project;
import com.example.backend.research.enums.ResearchDepth;
import com.example.backend.research.enums.RunStatus;
import com.example.backend.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "research_runs", indexes = {
    @Index(name = "idx_research_runs_project_id", columnList = "project_id"),
    @Index(name = "idx_research_runs_status", columnList = "status")
})
public class ResearchRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RunStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResearchDepth depth;

    @Column(name = "academic_enabled", nullable = false)
    private boolean academicEnabled;

    @Column(name = "plan_json", columnDefinition = "TEXT")
    private String planJson;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "tokens_used", nullable = false)
    private long tokensUsed = 0;

    @Column(name = "total_sources_found", nullable = false)
    private int totalSourcesFound = 0;

    @Column(name = "total_claims_extracted", nullable = false)
    private int totalClaimsExtracted = 0;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ResearchRun() {}

    public ResearchRun(Project project, String topic, ResearchDepth depth, boolean academicEnabled, User createdBy) {
        this.project = project;
        this.topic = topic;
        this.depth = depth;
        this.academicEnabled = academicEnabled;
        this.createdBy = createdBy;
        this.status = RunStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = RunStatus.PENDING;
        }
        if (this.depth == null) {
            this.depth = ResearchDepth.STANDARD;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public RunStatus getStatus() { return status; }
    public void setStatus(RunStatus status) { this.status = status; }

    public ResearchDepth getDepth() { return depth; }
    public void setDepth(ResearchDepth depth) { this.depth = depth; }

    public boolean isAcademicEnabled() { return academicEnabled; }
    public void setAcademicEnabled(boolean academicEnabled) { this.academicEnabled = academicEnabled; }

    public String getPlanJson() { return planJson; }
    public void setPlanJson(String planJson) { this.planJson = planJson; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public long getTokensUsed() { return tokensUsed; }
    public void setTokensUsed(long tokensUsed) { this.tokensUsed = tokensUsed; }

    public int getTotalSourcesFound() { return totalSourcesFound; }
    public void setTotalSourcesFound(int totalSourcesFound) { this.totalSourcesFound = totalSourcesFound; }

    public int getTotalClaimsExtracted() { return totalClaimsExtracted; }
    public void setTotalClaimsExtracted(int totalClaimsExtracted) { this.totalClaimsExtracted = totalClaimsExtracted; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
