package com.example.backend.research.service;

import com.example.backend.llm.LlmProvider;
import com.example.backend.llm.LlmResponse;
import com.example.backend.llm.dto.ResearchPlanDto;
import com.example.backend.research.entity.ResearchRun;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ResearchPlannerService {

    private static final Logger log = LoggerFactory.getLogger(ResearchPlannerService.class);
    private final LlmProvider llmProvider;
    private final ObjectMapper objectMapper;

    public ResearchPlannerService(LlmProvider llmProvider, ObjectMapper objectMapper) {
        this.llmProvider = llmProvider;
        this.objectMapper = objectMapper;
    }

    public ResearchPlanDto planResearch(ResearchRun run) {
        log.info("Generating research plan for topic: '{}', depth: {}", run.getTopic(), run.getDepth());

        String systemPrompt = "You are a senior research analyst and research planner. " +
                "Given a research topic, formulate a clear guiding hypothesis and decompose the topic into 4-6 targeted, analytical subquestions. " +
                "For each subquestion, formulate 2-3 precise search queries and specify the focus area.";

        String userPrompt = "Research Topic: " + run.getTopic() + "\n" +
                "Research Depth: " + run.getDepth() + "\n" +
                "Include Academic Inquiries: " + run.isAcademicEnabled();

        LlmResponse<ResearchPlanDto> planResponse = llmProvider.generateStructured(userPrompt, systemPrompt, ResearchPlanDto.class);
        ResearchPlanDto plan = planResponse.data();

        // Validation & fallback ensuring plan integrity
        if (plan == null || plan.getSubquestions() == null || plan.getSubquestions().isEmpty()) {
            log.warn("Model returned empty research plan, generating structured default plan");
            plan = createFallbackPlan(run.getTopic());
        }

        try {
            run.setPlanJson(objectMapper.writeValueAsString(plan));
        } catch (Exception e) {
            log.error("Failed to serialize research plan to JSON: {}", e.getMessage());
            run.setPlanJson("{}");
        }

        run.setTokensUsed(run.getTokensUsed() + planResponse.totalTokens());
        return plan;
    }

    private ResearchPlanDto createFallbackPlan(String topic) {
        List<ResearchPlanDto.SubQuestion> list = new ArrayList<>();
        list.add(new ResearchPlanDto.SubQuestion(
                "sq-1",
                "What are the foundational concepts and current state of " + topic + "?",
                List.of(topic + " overview", topic + " current state"),
                "Background & State of the Art"
        ));
        list.add(new ResearchPlanDto.SubQuestion(
                "sq-2",
                "What are the major industry developments, policies, and investments in " + topic + "?",
                List.of(topic + " policy investments", topic + " market analysis"),
                "Commercial & Policy Landscape"
        ));
        list.add(new ResearchPlanDto.SubQuestion(
                "sq-3",
                "What are the critical challenges, limitations, and future outlook for " + topic + "?",
                List.of(topic + " challenges limitations", topic + " future roadmap"),
                "Challenges & Opportunities"
        ));
        return new ResearchPlanDto(topic, "Comprehensive evidence-based assessment of " + topic, list);
    }
}
