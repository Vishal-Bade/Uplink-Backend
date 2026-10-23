package com.example.backend.research.dto;

import com.example.backend.research.entity.Citation;
import com.example.backend.research.entity.Report;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReportResponse(
        UUID id,
        UUID runId,
        UUID projectId,
        String title,
        String executiveSummary,
        String objectivesScope,
        String methodology,
        String keyFindings,
        String detailedAnalysis,
        String contradictionsLimitations,
        String conclusions,
        String citationsFormatted,
        List<CitationDto> citations,
        int version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record CitationDto(
            UUID id,
            int citationNumber,
            String citationKey,
            String sourceUrl,
            String sourceTitle,
            String formattedIeee,
            String formattedApa
    ) {
        public static CitationDto from(Citation c) {
            return new CitationDto(
                    c.getId(),
                    c.getCitationNumber(),
                    c.getCitationKey(),
                    c.getSource() != null ? c.getSource().getUrl() : null,
                    c.getSource() != null ? c.getSource().getTitle() : null,
                    c.getFormattedIeee(),
                    c.getFormattedApa()
            );
        }
    }

    public static ReportResponse from(Report r, List<Citation> citations) {
        List<CitationDto> citationDtos = citations != null ? citations.stream().map(CitationDto::from).toList() : List.of();
        return new ReportResponse(
                r.getId(),
                r.getRun() != null ? r.getRun().getId() : null,
                r.getProject() != null ? r.getProject().getId() : null,
                r.getTitle(),
                r.getExecutiveSummary(),
                r.getObjectivesScope(),
                r.getMethodology(),
                r.getKeyFindings(),
                r.getDetailedAnalysis(),
                r.getContradictionsLimitations(),
                r.getConclusions(),
                r.getCitationsFormatted(),
                citationDtos,
                r.getVersion(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
