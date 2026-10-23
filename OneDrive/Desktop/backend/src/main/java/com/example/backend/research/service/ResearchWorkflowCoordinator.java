package com.example.backend.research.service;

import com.example.backend.llm.dto.ResearchPlanDto;
import com.example.backend.research.entity.*;
import com.example.backend.research.enums.RunStatus;
import com.example.backend.research.enums.StepStatus;
import com.example.backend.research.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ResearchWorkflowCoordinator {

    private static final Logger log = LoggerFactory.getLogger(ResearchWorkflowCoordinator.class);

    private final ResearchRunRepository researchRunRepository;
    private final ResearchStepRepository researchStepRepository;
    private final SourceRepository sourceRepository;
    private final ResearchPlannerService plannerService;
    private final WebSearchService webSearchService;
    private final AcademicSearchService academicSearchService;
    private final WebPageExtractionService extractionService;
    private final DocumentProcessingService documentProcessingService;
    private final EvidenceRetrievalService retrievalService;
    private final ClaimExtractionService claimExtractionService;
    private final ClaimVerificationService claimVerificationService;
    private final ResearchSynthesisService synthesisService;
    private final CitationService citationService;
    private final ReportExportService exportService;
    private final SseEmitterService sseEmitterService;

    public ResearchWorkflowCoordinator(
            ResearchRunRepository researchRunRepository,
            ResearchStepRepository researchStepRepository,
            SourceRepository sourceRepository,
            ResearchPlannerService plannerService,
            WebSearchService webSearchService,
            AcademicSearchService academicSearchService,
            WebPageExtractionService extractionService,
            DocumentProcessingService documentProcessingService,
            EvidenceRetrievalService retrievalService,
            ClaimExtractionService claimExtractionService,
            ClaimVerificationService claimVerificationService,
            ResearchSynthesisService synthesisService,
            CitationService citationService,
            ReportExportService exportService,
            SseEmitterService sseEmitterService) {
        this.researchRunRepository = researchRunRepository;
        this.researchStepRepository = researchStepRepository;
        this.sourceRepository = sourceRepository;
        this.plannerService = plannerService;
        this.webSearchService = webSearchService;
        this.academicSearchService = academicSearchService;
        this.extractionService = extractionService;
        this.documentProcessingService = documentProcessingService;
        this.retrievalService = retrievalService;
        this.claimExtractionService = claimExtractionService;
        this.claimVerificationService = claimVerificationService;
        this.synthesisService = synthesisService;
        this.citationService = citationService;
        this.exportService = exportService;
        this.sseEmitterService = sseEmitterService;
    }

    @Async("researchTaskExecutor")
    public void executeResearchPipelineAsync(UUID runId) {
        executeResearchPipeline(runId);
    }

    public void executeResearchPipeline(UUID runId) {
        log.info("Executing research pipeline for run: {}", runId);
        ResearchRun run = researchRunRepository.findById(runId).orElse(null);
        if (run == null) {
            log.error("Cannot find research run with ID: {}", runId);
            return;
        }

        try {
            run.setStatus(RunStatus.PLANNING);
            run.setStartedAt(LocalDateTime.now());
            researchRunRepository.save(run);
            sseEmitterService.publishEvent(runId, "run.created", "Research run initialized for topic: " + run.getTopic());

            // 1. Research Planning
            checkCancellation(run);
            ResearchStep planStep = createStep(run, "PLANNING", 1);
            sseEmitterService.publishEvent(runId, "planning.started", "Generating research plan and analytical subquestions");

            ResearchPlanDto plan = plannerService.planResearch(run);
            completeStep(planStep, "Formulated " + plan.getSubquestions().size() + " subquestions.");

            // 2. Web & Academic Source Search
            checkCancellation(run);
            run.setStatus(RunStatus.SEARCHING);
            researchRunRepository.save(run);
            ResearchStep searchStep = createStep(run, "SEARCHING", 2);

            List<String> allQueries = new ArrayList<>();
            for (ResearchPlanDto.SubQuestion sq : plan.getSubquestions()) {
                allQueries.addAll(sq.getSearchQueries());
            }

            int sourceLimit = switch (run.getDepth()) {
                case QUICK -> 4;
                case STANDARD -> 8;
                case DEEP -> 15;
            };

            List<Source> sources = new ArrayList<>(webSearchService.searchAndCollectSources(run, allQueries, sourceLimit));
            if (run.isAcademicEnabled()) {
                sources.addAll(academicSearchService.searchAcademicSources(run, allQueries, 4));
            }
            completeStep(searchStep, "Found and persisted " + sources.size() + " unique sources.");
            sseEmitterService.publishEvent(runId, "search.progress", "Retrieved " + sources.size() + " authoritative sources");

            // 3. Web Page Content Extraction & Cleaning
            checkCancellation(run);
            run.setStatus(RunStatus.EXTRACTING);
            researchRunRepository.save(run);
            ResearchStep extractStep = createStep(run, "EXTRACTING", 3);

            for (Source s : sources) {
                extractionService.extractAndCleanContent(s);
            }
            completeStep(extractStep, "Extracted and sanitized text content from all sources.");

            // 4. Text Chunking & Embeddings
            checkCancellation(run);
            run.setStatus(RunStatus.CHUNKING);
            researchRunRepository.save(run);
            ResearchStep chunkStep = createStep(run, "CHUNKING", 4);

            List<DocumentChunk> allChunks = new ArrayList<>();
            for (Source s : sources) {
                allChunks.addAll(documentProcessingService.processSourceIntoChunks(run, s));
            }
            completeStep(chunkStep, "Generated " + allChunks.size() + " embedded document chunks.");
            sseEmitterService.publishEvent(runId, "documents.processed", "Processed " + allChunks.size() + " document chunks with vector embeddings");

            // 5. Evidence Retrieval
            checkCancellation(run);
            run.setStatus(RunStatus.RETRIEVING);
            researchRunRepository.save(run);
            ResearchStep retrieveStep = createStep(run, "RETRIEVING", 5);

            List<EvidenceRetrievalService.ScoredChunk> topEvidence = new ArrayList<>();
            for (ResearchPlanDto.SubQuestion sq : plan.getSubquestions()) {
                topEvidence.addAll(retrievalService.retrieveRelevantChunks(run.getId(), sq.getQuestion(), 3));
            }
            String assembledContext = retrievalService.assembleContext(topEvidence);
            completeStep(retrieveStep, "Retrieved top grounded evidence passages across subquestions.");

            // 6. Claim Extraction & Evidence Linking
            checkCancellation(run);
            ResearchStep claimStep = createStep(run, "CLAIM_EXTRACTION", 6);
            List<Claim> extractedClaims = claimExtractionService.extractClaims(run, assembledContext, sources);
            completeStep(claimStep, "Extracted " + extractedClaims.size() + " atomic factual claims.");

            // 7. Claim Verification
            checkCancellation(run);
            run.setStatus(RunStatus.VERIFYING);
            researchRunRepository.save(run);
            ResearchStep verifyStep = createStep(run, "VERIFYING", 7);

            List<Claim> verifiedClaims = claimVerificationService.verifyClaims(extractedClaims, assembledContext, sources);
            completeStep(verifyStep, "Verified " + verifiedClaims.size() + " claims (SUPPORTED, PARTIALLY_SUPPORTED, CONTRADICTED, UNVERIFIED).");
            sseEmitterService.publishEvent(runId, "verification.completed", "Factual verification complete for " + verifiedClaims.size() + " claims");

            // 8. Grounded Report Synthesis
            checkCancellation(run);
            run.setStatus(RunStatus.SYNTHESIZING);
            researchRunRepository.save(run);
            ResearchStep synthStep = createStep(run, "SYNTHESIZING", 8);

            Report report = synthesisService.synthesizeReport(run, plan, verifiedClaims, sources, assembledContext);
            completeStep(synthStep, "Synthesized multi-section research report.");
            sseEmitterService.publishEvent(runId, "report.generated", "Structured research report synthesized");

            // 9. Citations (IEEE & APA)
            ResearchStep citationStep = createStep(run, "CITATIONS", 9);
            citationService.generateAndPersistCitations(report, sources);
            completeStep(citationStep, "Generated IEEE and APA citation bibliography.");

            // 10. PDF and PPTX Artifact Exports
            ResearchStep exportStep = createStep(run, "EXPORTING", 10);
            try {
                exportService.exportPdf(report);
                exportService.exportPptx(report);
                completeStep(exportStep, "Exported PDF report and PPTX presentation slides.");
                sseEmitterService.publishEvent(runId, "export.completed", "Exported PDF and PPTX presentation artifacts");
            } catch (Exception e) {
                log.warn("Artifact export failed: {}", e.getMessage());
                failStep(exportStep, e.getMessage());
            }

            // Mark Run Completed
            run.setStatus(RunStatus.COMPLETED);
            run.setCompletedAt(LocalDateTime.now());
            researchRunRepository.save(run);
            sseEmitterService.publishEvent(runId, "run.completed", "Autonomous research pipeline completed successfully");
            log.info("Research pipeline finished successfully for run {}", runId);

        } catch (CancellationException e) {
            log.warn("Research run {} was cancelled by user", runId);
            run.setStatus(RunStatus.CANCELLED);
            run.setCompletedAt(LocalDateTime.now());
            run.setErrorMessage("Execution cancelled by user.");
            researchRunRepository.save(run);
            sseEmitterService.publishEvent(runId, "run.failed", "Research run was cancelled.");
        } catch (Exception e) {
            log.error("Research pipeline failed for run {}: {}", runId, e.getMessage(), e);
            run.setStatus(RunStatus.FAILED);
            run.setCompletedAt(LocalDateTime.now());
            run.setErrorMessage(e.getMessage());
            researchRunRepository.save(run);
            sseEmitterService.publishEvent(runId, "run.failed", "Pipeline failed: " + e.getMessage());
        }
    }

    private void checkCancellation(ResearchRun run) {
        ResearchRun latest = researchRunRepository.findById(run.getId()).orElse(run);
        if (latest.getStatus() == RunStatus.CANCELLED) {
            throw new CancellationException("Run was cancelled");
        }
    }

    private ResearchStep createStep(ResearchRun run, String stepName, int order) {
        ResearchStep step = new ResearchStep(run, stepName, order);
        step.setStatus(StepStatus.IN_PROGRESS);
        step.setStartedAt(LocalDateTime.now());
        return researchStepRepository.save(step);
    }

    private void completeStep(ResearchStep step, String outputData) {
        step.setStatus(StepStatus.COMPLETED);
        step.setOutputData(outputData);
        step.setCompletedAt(LocalDateTime.now());
        researchStepRepository.save(step);
    }

    private void failStep(ResearchStep step, String errorMessage) {
        step.setStatus(StepStatus.FAILED);
        step.setErrorMessage(errorMessage);
        step.setCompletedAt(LocalDateTime.now());
        researchStepRepository.save(step);
    }

    public static class CancellationException extends RuntimeException {
        public CancellationException(String message) {
            super(message);
        }
    }
}
