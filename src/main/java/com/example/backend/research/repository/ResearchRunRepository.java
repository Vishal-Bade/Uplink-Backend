package com.example.backend.research.repository;

import com.example.backend.research.entity.ResearchRun;
import com.example.backend.research.enums.RunStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResearchRunRepository extends JpaRepository<ResearchRun, UUID> {
    List<ResearchRun> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
    Page<ResearchRun> findByProjectId(UUID projectId, Pageable pageable);
    List<ResearchRun> findByStatus(RunStatus status);
    Optional<ResearchRun> findByIdAndCreatedById(UUID id, UUID createdById);
}
