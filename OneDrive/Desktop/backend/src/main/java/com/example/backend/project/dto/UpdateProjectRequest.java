package com.example.backend.project.dto;

import com.example.backend.research.enums.ProjectStatus;

public class UpdateProjectRequest {
    private String name;
    private String description;
    private String researchObjectives;
    private ProjectStatus status;

    public UpdateProjectRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getResearchObjectives() { return researchObjectives; }
    public void setResearchObjectives(String researchObjectives) { this.researchObjectives = researchObjectives; }

    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }
}
