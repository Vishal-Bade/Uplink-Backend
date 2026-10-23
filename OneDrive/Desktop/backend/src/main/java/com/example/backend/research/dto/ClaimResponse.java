package com.example.backend.research.dto;

import com.example.backend.research.entity.Claim;
import com.example.backend.research.entity.ClaimEvidence;
import com.example.backend.research.enums.ClaimStatus;
import com.example.backend.research.enums.EvidenceType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ClaimResponse(
        UUID id,
        UUID runId,
        String claimText,
        String category,
        ClaimStatus verificationStatus,
        String verificationExplanation,
        List<EvidenceDto> evidences,
        LocalDateTime createdAt
) {
    public record EvidenceDto(
            UUID id,
            String sourceUrl,
            String sourceTitle,
            String evidenceText,
            EvidenceType evidenceType,
            String sectionOrPage
    ) {
        public static EvidenceDto from(ClaimEvidence ce) {
            return new EvidenceDto(
                    ce.getId(),
                    ce.getSource() != null ? ce.getSource().getUrl() : null,
                    ce.getSource() != null ? ce.getSource().getTitle() : null,
                    ce.getEvidenceText(),
                    ce.getEvidenceType(),
                    ce.getSectionOrPage()
            );
        }
    }

    public static ClaimResponse from(Claim claim, List<ClaimEvidence> evidences) {
        List<EvidenceDto> evidenceDtos = evidences != null ? evidences.stream().map(EvidenceDto::from).toList() : List.of();
        return new ClaimResponse(
                claim.getId(),
                claim.getRun() != null ? claim.getRun().getId() : null,
                claim.getClaimText(),
                claim.getCategory(),
                claim.getVerificationStatus(),
                claim.getVerificationExplanation(),
                evidenceDtos,
                claim.getCreatedAt()
        );
    }
}
