package com.example.backend.research.entity;

import com.example.backend.research.enums.ArtifactType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "artifacts", indexes = {
    @Index(name = "idx_artifacts_run_id", columnList = "run_id")
})
public class Artifact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private ResearchRun run;

    @Enumerated(EnumType.STRING)
    @Column(name = "artifact_type", nullable = false, length = 20)
    private ArtifactType artifactType;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Artifact() {}

    public Artifact(ResearchRun run, ArtifactType artifactType, String fileName, long fileSize, String mimeType, String storagePath) {
        this.run = run;
        this.artifactType = artifactType;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.storagePath = storagePath;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ResearchRun getRun() { return run; }
    public void setRun(ResearchRun run) { this.run = run; }

    public ArtifactType getArtifactType() { return artifactType; }
    public void setArtifactType(ArtifactType artifactType) { this.artifactType = artifactType; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
