package com.example.backend.workspace.dto;

import com.example.backend.workspace.entity.Workspace;
import java.time.LocalDateTime;
import java.util.UUID;

public record WorkspaceResponse(
        UUID id,
        String name,
        String description,
        UUID ownerId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static WorkspaceResponse from(Workspace ws) {
        return new WorkspaceResponse(
                ws.getId(),
                ws.getName(),
                ws.getDescription(),
                ws.getOwner() != null ? ws.getOwner().getId() : null,
                ws.getCreatedAt(),
                ws.getUpdatedAt()
        );
    }
}
