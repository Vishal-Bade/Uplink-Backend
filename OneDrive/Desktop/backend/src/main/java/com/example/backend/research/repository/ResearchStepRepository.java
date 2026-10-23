package com.example.backend.research.repository;

import com.example.backend.research.entity.ResearchStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResearchStepRepository extends JpaRepository<ResearchStep, UUID> {
    List<ResearchStep> findByRunIdOrderByStepOrderAsc(UUID runId);
    Optional<ResearchStep> findByRunIdAndStepName(UUID runId, String stepName);
}
