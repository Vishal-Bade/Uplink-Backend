package com.example.backend.research.repository;

import com.example.backend.research.entity.Artifact;
import com.example.backend.research.enums.ArtifactType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArtifactRepository extends JpaRepository<Artifact, UUID> {
    List<Artifact> findByRunId(UUID runId);
    Optional<Artifact> findByRunIdAndArtifactType(UUID runId, ArtifactType artifactType);
}
