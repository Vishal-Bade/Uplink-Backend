package com.example.backend.llm;

import com.example.backend.common.util.VectorMath;
import com.example.backend.llm.dto.ExtractedClaimsDto;
import com.example.backend.llm.dto.ResearchPlanDto;
import com.example.backend.llm.dto.SynthesizedReportDto;
import com.example.backend.llm.dto.VerificationResultDto;
import com.example.backend.research.enums.ClaimStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class MockLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(MockLlmProvider.class);

    @Override
    public LlmResponse<String> generateText(String prompt, String systemInstruction) {
        log.info("MockLlmProvider generating text response for prompt length: {}", prompt.length());
        return LlmResponse.of("Autonomous Research Workspace analysis grounded in evidence for: " + prompt.substring(0, Math.min(100, prompt.length())), 120, 240, "mock-llm-v1");
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> LlmResponse<T> generateStructured(String prompt, String systemInstruction, Class<T> responseClass) {
        log.info("MockLlmProvider generating structured response for class: {}", responseClass.getSimpleName());

        if (responseClass.equals(ResearchPlanDto.class)) {
            ResearchPlanDto plan = createMockResearchPlan(prompt);
            return (LlmResponse<T>) LlmResponse.of(plan, 150, 450, "mock-llm-v1");
        } else if (responseClass.equals(ExtractedClaimsDto.class)) {
            ExtractedClaimsDto claims = createMockClaims(prompt);
            return (LlmResponse<T>) LlmResponse.of(claims, 200, 500, "mock-llm-v1");
        } else if (responseClass.equals(VerificationResultDto.class)) {
            VerificationResultDto verification = createMockVerification(prompt);
            return (LlmResponse<T>) LlmResponse.of(verification, 250, 600, "mock-llm-v1");
        } else if (responseClass.equals(SynthesizedReportDto.class)) {
            SynthesizedReportDto report = createMockReport(prompt);
            return (LlmResponse<T>) LlmResponse.of(report, 400, 1200, "mock-llm-v1");
        }

        try {
            return (LlmResponse<T>) LlmResponse.of(responseClass.getDeclaredConstructor().newInstance());
        } catch (Exception e) {
            throw new RuntimeException("Cannot instantiate mock response for " + responseClass.getName(), e);
        }
    }

    @Override
    public float[] generateEmbedding(String text) {
        return VectorMath.generatePseudoEmbedding(text, 1536);
    }

    @Override
    public String getProviderName() {
        return "mock-llm-engine";
    }

    private ResearchPlanDto createMockResearchPlan(String prompt) {
        String topic = extractTopic(prompt);
        List<ResearchPlanDto.SubQuestion> subquestions = new ArrayList<>();

        subquestions.add(new ResearchPlanDto.SubQuestion(
                "sq-1",
                "What is the current policy landscape and government incentive framework for " + topic + "?",
                List.of(topic + " government policy incentives", topic + " regulatory framework 2024"),
                "Policy and Governance"
        ));

        subquestions.add(new ResearchPlanDto.SubQuestion(
                "sq-2",
                "What are the major industry investments, market size, and infrastructure developments in " + topic + "?",
                List.of(topic + " major investments capital expenditure", topic + " industry infrastructure market growth"),
                "Industry & Investment"
        ));

        subquestions.add(new ResearchPlanDto.SubQuestion(
                "sq-3",
                "What are the primary technical bottlenecks, supply chain dependencies, and ecosystem challenges?",
                List.of(topic + " supply chain vulnerabilities bottlenecks", topic + " technical ecosystem challenges"),
                "Challenges & Risks"
        ));

        subquestions.add(new ResearchPlanDto.SubQuestion(
                "sq-4",
                "What are the emerging future opportunities and strategic growth vectors for the next decade?",
                List.of(topic + " future roadmap opportunities 2030", topic + " technological advancements next generation"),
                "Strategic Outlook"
        ));

        return new ResearchPlanDto(
                topic,
                "A comprehensive investigation into " + topic + " indicates rapid growth driven by strategic policies, accompanied by supply chain challenges and significant technological opportunities.",
                subquestions
        );
    }

    private ExtractedClaimsDto createMockClaims(String prompt) {
        String topic = extractTopic(prompt);
        List<ExtractedClaimsDto.ClaimItem> items = new ArrayList<>();

        items.add(new ExtractedClaimsDto.ClaimItem(
                "Government incentives and specialized funding initiatives have committed billions in subsidies to accelerate domestic capability in " + topic + ".",
                "Policy",
                "https://example.com/policy-initiatives",
                "Official records confirm multi-billion dollar capital subsidy programs targeted at domestic production hubs.",
                "Section 2.1"
        ));

        items.add(new ExtractedClaimsDto.ClaimItem(
                "Major global technology conglomerates have announced joint ventures and advanced facility construction for " + topic + ".",
                "Investment",
                "https://example.com/industry-report",
                "Leading enterprise consortia committed capital towards building state-of-the-art manufacturing and research facilities.",
                "Executive Summary"
        ));

        items.add(new ExtractedClaimsDto.ClaimItem(
                "Specialized raw material supply chains and advanced talent shortages remain critical bottlenecks limiting near-term self-sufficiency.",
                "Challenges",
                "https://example.com/supply-chain-analysis",
                "A persistent shortage of advanced technical talent and dependence on foreign specialized precursor materials creates operational risks.",
                "Page 14"
        ));

        items.add(new ExtractedClaimsDto.ClaimItem(
                "Long-term domestic demand is projected to triple by 2030, driven by consumer electronics, automotive electrification, and AI workloads.",
                "Market Growth",
                "https://example.com/market-outlook-2030",
                "Market projections indicate a three-fold increase in domestic consumption fueled by automotive and industrial electronics.",
                "Forecast Chapter"
        ));

        return new ExtractedClaimsDto(items);
    }

    private VerificationResultDto createMockVerification(String prompt) {
        List<VerificationResultDto.VerifiedClaimItem> list = new ArrayList<>();

        VerificationResultDto.VerifiedClaimItem c1 = new VerificationResultDto.VerifiedClaimItem(
                "Government incentives and specialized funding initiatives have committed capital subsidies to accelerate domestic capability.",
                ClaimStatus.SUPPORTED,
                "Supported by official policy whitepapers and budget allocations detailing direct capital expenditure subsidies."
        );
        c1.getSupportingPassages().add("Public gazette records allocate financial assistance up to 50% of project cost.");

        VerificationResultDto.VerifiedClaimItem c2 = new VerificationResultDto.VerifiedClaimItem(
                "Global technology conglomerates have finalized facility construction.",
                ClaimStatus.PARTIALLY_SUPPORTED,
                "Groundbreaking ceremonies and joint venture MoUs are confirmed, though full operational fabrication remains pending completion."
        );
        c2.getSupportingPassages().add("Joint venture agreements and environmental clearances approved.");
        c2.getContradictingPassages().add("Full commercial volume output has not yet reached commercial yields as of current quarter.");

        VerificationResultDto.VerifiedClaimItem c3 = new VerificationResultDto.VerifiedClaimItem(
                "Domestic production currently satisfies over 80% of total national requirements.",
                ClaimStatus.CONTRADICTED,
                "Empirical trade and import data demonstrate that domestic supply currently covers less than 20% of aggregate demand, relying heavily on imports."
        );
        c3.getContradictingPassages().add("Import data indicates over 85% of high-end components continue to be sourced internationally.");

        list.add(c1);
        list.add(c2);
        list.add(c3);

        return new VerificationResultDto(list);
    }

    private SynthesizedReportDto createMockReport(String prompt) {
        String topic = extractTopic(prompt);
        SynthesizedReportDto report = new SynthesizedReportDto();
        report.setTitle("Comprehensive Strategic Assessment: " + topic);
        report.setExecutiveSummary("This autonomous research investigation evaluates the current status, policy frameworks, commercial investments, ecosystem vulnerabilities, and strategic trajectory of " + topic + ". The findings indicate strong policy-driven momentum backed by capital subsidies, balanced by structural dependencies on international supply chains and human capital availability.");
        report.setObjectivesScope("The primary objective of this report is to provide an evidence-grounded analysis of " + topic + ", examining regulatory structures, private capital commitments, supply chain resilience, and long-term economic impact.");
        report.setMethodology("Autonomous multi-source evidence extraction, cross-source claim corroboration, hybrid semantic vector retrieval, and rigorous verification against empirical academic and industry documentation.");
        report.setKeyFindings("1. Multi-billion dollar incentive schemes have catalyzed initial facility establishment.\n2. Strategic joint ventures bridge technological IP gaps with international leaders.\n3. Talent pipeline constraints and precursor supply chains represent primary operational vulnerabilities.\n4. Market demand is poised for compound annual growth exceeding 15% through 2030.");
        report.setDetailedAnalysis("Detailed examination reveals that governmental policy instruments have successfully lowered barriers to capital investment. However, establishing full-stack self-sufficiency requires simultaneous cultivation of secondary equipment suppliers, specialized chemicals, and specialized technical university curricula. Cross-border partnerships have proven essential in transferring foundational manufacturing patents and process knowledge.");
        report.setContradictionsLimitations("While official communications report aggressive timelines, independent industry audits caution that initial pilot yields may take longer to stabilize than publicly forecasted. Furthermore, supply chain disruptions in rare earth minerals pose volatility risks not fully mitigated by domestic reserves.");
        report.setConclusions("The development of " + topic + " is on an upward trajectory. Sustained execution of policy commitments, combined with targeted investments in domestic research universities and upstream supplier networks, will be decisive in achieving strategic resilience by 2030.");
        report.setCitedUrls(List.of(
                "https://example.com/policy-initiatives",
                "https://example.com/industry-report",
                "https://example.com/supply-chain-analysis",
                "https://example.com/market-outlook-2030"
        ));
        return report;
    }

    private String extractTopic(String prompt) {
        if (prompt == null || prompt.isBlank()) return "Autonomous Research Domain";
        String p = prompt.replaceAll("(?i)(topic|plan|research|questions|subquestions|claims|verify|summarize|report):?", "").trim();
        return p.length() > 60 ? p.substring(0, 60) : p;
    }
}
