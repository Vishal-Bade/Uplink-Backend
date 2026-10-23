package com.example.backend.research.repository;

import com.example.backend.research.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {
    Optional<Report> findByRunId(UUID runId);
    List<Report> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
