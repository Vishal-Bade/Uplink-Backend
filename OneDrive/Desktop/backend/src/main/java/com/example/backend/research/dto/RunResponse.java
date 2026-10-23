package com.example.backend.research.dto;

import com.example.backend.research.entity.ResearchRun;
import com.example.backend.research.enums.ResearchDepth;
import com.example.backend.research.enums.RunStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record RunResponse(
        UUID id,
        UUID projectId,
        String topic,
        RunStatus status,
        ResearchDepth depth,
        boolean academicEnabled,
        String errorMessage,
        long tokensUsed,
        int totalSourcesFound,
        int totalClaimsExtracted,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {
    public static RunResponse from(ResearchRun run) {
        return new RunResponse(
                run.getId(),
                run.getProject() != null ? run.getProject().getId() : null,
                run.getTopic(),
                run.getStatus(),
                run.getDepth(),
                run.isAcademicEnabled(),
                run.getErrorMessage(),
                run.getTokensUsed(),
                run.getTotalSourcesFound(),
                run.getTotalClaimsExtracted(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getCreatedAt()
        );
    }
}
