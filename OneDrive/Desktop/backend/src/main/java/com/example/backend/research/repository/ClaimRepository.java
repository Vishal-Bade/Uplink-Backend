package com.example.backend.research.repository;

import com.example.backend.research.entity.Claim;
import com.example.backend.research.enums.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, UUID> {
    List<Claim> findByRunId(UUID runId);
    List<Claim> findByRunIdAndVerificationStatus(UUID runId, ClaimStatus status);
}
