package com.example.backend.research.repository;

import com.example.backend.research.entity.ClaimEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClaimEvidenceRepository extends JpaRepository<ClaimEvidence, UUID> {
    List<ClaimEvidence> findByClaimId(UUID claimId);

    @Query("SELECT ce FROM ClaimEvidence ce JOIN FETCH ce.source WHERE ce.claim.id = :claimId")
    List<ClaimEvidence> findByClaimIdWithSource(@Param("claimId") UUID claimId);

    @Query("SELECT ce FROM ClaimEvidence ce JOIN FETCH ce.source WHERE ce.claim.run.id = :runId")
    List<ClaimEvidence> findByRunIdWithSource(@Param("runId") UUID runId);
}
