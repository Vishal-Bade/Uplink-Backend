package com.example.backend.research.repository;

import com.example.backend.research.entity.ScheduledResearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduledResearchRepository extends JpaRepository<ScheduledResearch, UUID> {
    List<ScheduledResearch> findByProjectId(UUID projectId);
    List<ScheduledResearch> findByActiveTrue();
    List<ScheduledResearch> findByActiveTrueAndNextRunAtBefore(LocalDateTime time);
}
