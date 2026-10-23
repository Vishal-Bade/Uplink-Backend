package com.example.backend.project.dto;

import com.example.backend.project.entity.Project;
import com.example.backend.research.enums.ProjectStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        UUID workspaceId,
        String name,
        String description,
        String researchObjectives,
        ProjectStatus status,
        UUID createdById,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProjectResponse from(Project p) {
        return new ProjectResponse(
                p.getId(),
                p.getWorkspace() != null ? p.getWorkspace().getId() : null,
                p.getName(),
                p.getDescription(),
                p.getResearchObjectives(),
                p.getStatus(),
                p.getCreatedBy() != null ? p.getCreatedBy().getId() : null,
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
