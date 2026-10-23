package com.example.backend.llm.dto;

import java.util.ArrayList;
import java.util.List;

public class SynthesizedReportDto {
    private String title;
    private String executiveSummary;
    private String objectivesScope;
    private String methodology;
    private String keyFindings;
    private String detailedAnalysis;
    private String contradictionsLimitations;
    private String conclusions;
    private List<String> citedUrls = new ArrayList<>();

    public SynthesizedReportDto() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getExecutiveSummary() { return executiveSummary; }
    public void setExecutiveSummary(String executiveSummary) { this.executiveSummary = executiveSummary; }

    public String getObjectivesScope() { return objectivesScope; }
    public void setObjectivesScope(String objectivesScope) { this.objectivesScope = objectivesScope; }

    public String getMethodology() { return methodology; }
    public void setMethodology(String methodology) { this.methodology = methodology; }

    public String getKeyFindings() { return keyFindings; }
    public void setKeyFindings(String keyFindings) { this.keyFindings = keyFindings; }

    public String getDetailedAnalysis() { return detailedAnalysis; }
    public void setDetailedAnalysis(String detailedAnalysis) { this.detailedAnalysis = detailedAnalysis; }

    public String getContradictionsLimitations() { return contradictionsLimitations; }
    public void setContradictionsLimitations(String contradictionsLimitations) { this.contradictionsLimitations = contradictionsLimitations; }

    public String getConclusions() { return conclusions; }
    public void setConclusions(String conclusions) { this.conclusions = conclusions; }

    public List<String> getCitedUrls() { return citedUrls; }
    public void setCitedUrls(List<String> citedUrls) { this.citedUrls = citedUrls; }
}
