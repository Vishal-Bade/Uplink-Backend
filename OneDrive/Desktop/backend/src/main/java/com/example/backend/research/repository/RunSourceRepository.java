package com.example.backend.research.repository;

import com.example.backend.research.entity.RunSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RunSourceRepository extends JpaRepository<RunSource, UUID> {
    List<RunSource> findByRunId(UUID runId);
    Optional<RunSource> findByRunIdAndSourceId(UUID runId, UUID sourceId);

    @Query("SELECT rs FROM RunSource rs JOIN FETCH rs.source WHERE rs.run.id = :runId ORDER BY rs.relevanceScore DESC NULLS LAST")
    List<RunSource> findByRunIdWithSource(@Param("runId") UUID runId);
}
