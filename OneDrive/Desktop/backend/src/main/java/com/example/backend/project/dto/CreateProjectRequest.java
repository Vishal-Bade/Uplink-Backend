package com.example.backend.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class CreateProjectRequest {

    @NotNull(message = "Workspace ID is required")
    private UUID workspaceId;

    @NotBlank(message = "Project name is required")
    @Size(max = 150, message = "Project name cannot exceed 150 characters")
    private String name;

    private String description;
    private String researchObjectives;

    public CreateProjectRequest() {}

    public CreateProjectRequest(UUID workspaceId, String name, String description, String researchObjectives) {
        this.workspaceId = workspaceId;
        this.name = name;
        this.description = description;
        this.researchObjectives = researchObjectives;
    }

    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getResearchObjectives() { return researchObjectives; }
    public void setResearchObjectives(String researchObjectives) { this.researchObjectives = researchObjectives; }
}
