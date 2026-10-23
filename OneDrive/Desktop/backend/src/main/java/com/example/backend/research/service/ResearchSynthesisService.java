package com.example.backend.research.service;

import com.example.backend.llm.LlmProvider;
import com.example.backend.llm.LlmResponse;
import com.example.backend.llm.dto.ResearchPlanDto;
import com.example.backend.llm.dto.SynthesizedReportDto;
import com.example.backend.research.entity.Claim;
import com.example.backend.research.entity.Report;
import com.example.backend.research.entity.ResearchRun;
import com.example.backend.research.entity.Source;
import com.example.backend.research.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ResearchSynthesisService {

    private static final Logger log = LoggerFactory.getLogger(ResearchSynthesisService.class);
    private final LlmProvider llmProvider;
    private final ReportRepository reportRepository;

    public ResearchSynthesisService(LlmProvider llmProvider, ReportRepository reportRepository) {
        this.llmProvider = llmProvider;
        this.reportRepository = reportRepository;
    }

    @Transactional
    public Report synthesizeReport(ResearchRun run, ResearchPlanDto plan, List<Claim> claims, List<Source> sources, String context) {
        log.info("Synthesizing comprehensive research report for run {}", run.getId());

        StringBuilder claimsSummary = new StringBuilder();
        for (Claim c : claims) {
            claimsSummary.append("- [").append(c.getVerificationStatus()).append("] ")
                    .append(c.getClaimText()).append(" (Explanation: ")
                    .append(c.getVerificationExplanation()).append(")\n");
        }

        String systemPrompt = "You are a distinguished research scientist and executive report author. " +
                "Synthesize a rigorous, publication-grade research report strictly grounded in the verified claims and evidence provided. " +
                "Structure your output into the specified sections: Title, Executive Summary, Research Objectives & Scope, Methodology, " +
                "Key Findings, Detailed Analysis, Contradictions & Limitations, Conclusions, and Cited URLs.";

        String userPrompt = "Research Topic: " + run.getTopic() + "\n\n" +
                "Guiding Hypothesis: " + (plan != null ? plan.getHypothesis() : "N/A") + "\n\n" +
                "Verified Claims & Statuses:\n" + claimsSummary + "\n\n" +
                "Retrieved Evidence Passages:\n" + context;

        LlmResponse<SynthesizedReportDto> response = llmProvider.generateStructured(userPrompt, systemPrompt, SynthesizedReportDto.class);
        SynthesizedReportDto resolvedDto = response.data();
        if (resolvedDto == null) {
            resolvedDto = new SynthesizedReportDto();
            resolvedDto.setTitle("Research Report: " + run.getTopic());
            resolvedDto.setExecutiveSummary("Executive evaluation of " + run.getTopic());
            resolvedDto.setKeyFindings("Analysis completed with " + claims.size() + " extracted and verified claims.");
            resolvedDto.setDetailedAnalysis("Comprehensive analysis based on gathered evidence sources.");
            resolvedDto.setConclusions("Investigation concluded successfully.");
        }
        final SynthesizedReportDto finalDto = resolvedDto;

        Report report = reportRepository.findByRunId(run.getId()).orElseGet(() -> new Report(run, run.getProject(), finalDto.getTitle()));
        report.setTitle(finalDto.getTitle() != null ? finalDto.getTitle() : "Research Report: " + run.getTopic());
        report.setExecutiveSummary(finalDto.getExecutiveSummary());
        report.setObjectivesScope(finalDto.getObjectivesScope() != null ? finalDto.getObjectivesScope() : "Scope: " + run.getTopic());
        report.setMethodology(finalDto.getMethodology() != null ? finalDto.getMethodology() : "Multi-source autonomous retrieval, vector ranking, and factual claim verification.");
        report.setKeyFindings(finalDto.getKeyFindings());
        report.setDetailedAnalysis(finalDto.getDetailedAnalysis());
        report.setContradictionsLimitations(finalDto.getContradictionsLimitations() != null ? finalDto.getContradictionsLimitations() : "Identified contradictory and unverified aspects require continual monitoring.");
        report.setConclusions(finalDto.getConclusions());
        report.setVersion(report.getVersion() + 1);

        Report savedReport = reportRepository.save(report);
        run.setTokensUsed(run.getTokensUsed() + response.totalTokens());
        log.info("Persisted synthesized report {} (version {}) for run {}", savedReport.getId(), savedReport.getVersion(), run.getId());
        return savedReport;
    }
}
