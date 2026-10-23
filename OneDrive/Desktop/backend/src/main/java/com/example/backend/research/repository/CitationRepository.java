package com.example.backend.research.repository;

import com.example.backend.research.entity.Citation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CitationRepository extends JpaRepository<Citation, UUID> {
    List<Citation> findByReportIdOrderByCitationNumberAsc(UUID reportId);

    @Query("SELECT c FROM Citation c JOIN FETCH c.source WHERE c.report.id = :reportId ORDER BY c.citationNumber ASC")
    List<Citation> findByReportIdWithSource(@Param("reportId") UUID reportId);
}
