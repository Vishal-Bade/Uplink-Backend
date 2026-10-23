package com.example.backend.research.dto;

import com.example.backend.research.enums.ResearchDepth;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateRunRequest {

    @NotBlank(message = "Research topic is required")
    @Size(max = 1000, message = "Topic cannot exceed 1000 characters")
    private String topic;

    private ResearchDepth depth = ResearchDepth.STANDARD;
    private boolean academicEnabled = false;

    public CreateRunRequest() {}

    public CreateRunRequest(String topic, ResearchDepth depth, boolean academicEnabled) {
        this.topic = topic;
        this.depth = depth != null ? depth : ResearchDepth.STANDARD;
        this.academicEnabled = academicEnabled;
    }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public ResearchDepth getDepth() { return depth; }
    public void setDepth(ResearchDepth depth) { this.depth = depth; }

    public boolean isAcademicEnabled() { return academicEnabled; }
    public void setAcademicEnabled(boolean academicEnabled) { this.academicEnabled = academicEnabled; }
}
