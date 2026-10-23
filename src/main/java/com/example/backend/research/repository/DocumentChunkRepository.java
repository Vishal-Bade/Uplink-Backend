package com.example.backend.research.repository;

import com.example.backend.research.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {
    List<DocumentChunk> findByRunId(UUID runId);
    List<DocumentChunk> findBySourceId(UUID sourceId);

    @Query("SELECT dc FROM DocumentChunk dc JOIN FETCH dc.source WHERE dc.run.id = :runId")
    List<DocumentChunk> findByRunIdWithSource(@Param("runId") UUID runId);

    @Query("SELECT dc FROM DocumentChunk dc JOIN FETCH dc.source WHERE dc.document.project.id = :projectId")
    List<DocumentChunk> findByProjectIdWithSource(@Param("projectId") UUID projectId);
}
