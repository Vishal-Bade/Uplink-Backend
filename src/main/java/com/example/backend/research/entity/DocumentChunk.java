package com.example.backend.research.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_chunks", indexes = {
    @Index(name = "idx_document_chunks_run_id", columnList = "run_id"),
    @Index(name = "idx_document_chunks_source_id", columnList = "source_id")
})
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private DocumentEntity document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private Source source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id")
    private ResearchRun run;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "token_count", nullable = false)
    private int tokenCount = 0;

    @Column(name = "start_char_offset")
    private Integer startCharOffset;

    @Column(name = "end_char_offset")
    private Integer endCharOffset;

    @Column(columnDefinition = "TEXT")
    private String embedding;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public DocumentChunk() {}

    public DocumentChunk(ResearchRun run, Source source, int chunkIndex, String content, int tokenCount) {
        this.run = run;
        this.source = source;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.tokenCount = tokenCount;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public DocumentEntity getDocument() { return document; }
    public void setDocument(DocumentEntity document) { this.document = document; }

    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }

    public ResearchRun getRun() { return run; }
    public void setRun(ResearchRun run) { this.run = run; }

    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getTokenCount() { return tokenCount; }
    public void setTokenCount(int tokenCount) { this.tokenCount = tokenCount; }

    public Integer getStartCharOffset() { return startCharOffset; }
    public void setStartCharOffset(Integer startCharOffset) { this.startCharOffset = startCharOffset; }

    public Integer getEndCharOffset() { return endCharOffset; }
    public void setEndCharOffset(Integer endCharOffset) { this.endCharOffset = endCharOffset; }

    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
