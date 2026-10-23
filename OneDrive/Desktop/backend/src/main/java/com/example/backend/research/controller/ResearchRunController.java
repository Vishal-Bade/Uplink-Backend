package com.example.backend.research.controller;

import com.example.backend.common.exception.ResourceNotFoundException;
import com.example.backend.common.response.ApiResponse;
import com.example.backend.project.entity.Project;
import com.example.backend.research.dto.*;
import com.example.backend.research.entity.*;
import com.example.backend.research.enums.RunStatus;
import com.example.backend.research.repository.*;
import com.example.backend.research.service.ResearchWorkflowCoordinator;
import com.example.backend.research.service.SseEmitterService;
import com.example.backend.security.ProjectSecurityService;
import com.example.backend.user.entity.User;
import com.example.backend.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ResearchRunController {

    private final ProjectSecurityService securityService;
    private final UserRepository userRepository;
    private final ResearchRunRepository researchRunRepository;
    private final RunSourceRepository runSourceRepository;
    private final ClaimRepository claimRepository;
    private final ClaimEvidenceRepository claimEvidenceRepository;
    private final ReportRepository reportRepository;
    private final CitationRepository citationRepository;
    private final ResearchWorkflowCoordinator workflowCoordinator;
    private final SseEmitterService sseEmitterService;

    public ResearchRunController(
            ProjectSecurityService securityService,
            UserRepository userRepository,
            ResearchRunRepository researchRunRepository,
            RunSourceRepository runSourceRepository,
            ClaimRepository claimRepository,
            ClaimEvidenceRepository claimEvidenceRepository,
            ReportRepository reportRepository,
            CitationRepository citationRepository,
            ResearchWorkflowCoordinator workflowCoordinator,
            SseEmitterService sseEmitterService) {
        this.securityService = securityService;
        this.userRepository = userRepository;
        this.researchRunRepository = researchRunRepository;
        this.runSourceRepository = runSourceRepository;
        this.claimRepository = claimRepository;
        this.claimEvidenceRepository = claimEvidenceRepository;
        this.reportRepository = reportRepository;
        this.citationRepository = citationRepository;
        this.workflowCoordinator = workflowCoordinator;
        this.sseEmitterService = sseEmitterService;
    }

    @PostMapping("/projects/{projectId}/runs")
    public ResponseEntity<ApiResponse<RunResponse>> startResearchRun(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateRunRequest request) {
        User user = getUser(userId);
        Project project = securityService.validateAndGetProject(projectId, user);

        ResearchRun run = new ResearchRun(
                project,
                request.getTopic(),
                request.getDepth(),
                request.isAcademicEnabled(),
                user
        );
        run = researchRunRepository.save(run);

        // Submit research pipeline asynchronously to background executor
        workflowCoordinator.executeResearchPipelineAsync(run.getId());

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Research run queued successfully", RunResponse.from(run)));
    }

    @GetMapping("/runs/{runId}")
    public ResponseEntity<ApiResponse<RunResponse>> getRun(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID runId) {
        User user = getUser(userId);
        ResearchRun run = securityService.validateAndGetRun(runId, user);
        return ResponseEntity.ok(ApiResponse.success("Research run retrieved", RunResponse.from(run)));
    }

    @GetMapping("/runs/{runId}/sources")
    public ResponseEntity<ApiResponse<List<SourceResponse>>> getSources(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID runId) {
        User user = getUser(userId);
        securityService.validateAndGetRun(runId, user);

        List<SourceResponse> sources = runSourceRepository.findByRunIdWithSource(runId).stream()
                .map(SourceResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Sources retrieved", sources));
    }

    @GetMapping("/runs/{runId}/claims")
    public ResponseEntity<ApiResponse<List<ClaimResponse>>> getClaims(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID runId) {
        User user = getUser(userId);
        securityService.validateAndGetRun(runId, user);

        List<Claim> claims = claimRepository.findByRunId(runId);
        List<ClaimResponse> responses = claims.stream().map(claim -> {
            List<ClaimEvidence> evidences = claimEvidenceRepository.findByClaimIdWithSource(claim.getId());
            return ClaimResponse.from(claim, evidences);
        }).toList();

        return ResponseEntity.ok(ApiResponse.success("Claims retrieved", responses));
    }

    @GetMapping("/runs/{runId}/report")
    public ResponseEntity<ApiResponse<ReportResponse>> getReport(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID runId) {
        User user = getUser(userId);
        securityService.validateAndGetRun(runId, user);

        Report report = reportRepository.findByRunId(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not yet generated for run: " + runId));

        List<Citation> citations = citationRepository.findByReportIdWithSource(report.getId());
        return ResponseEntity.ok(ApiResponse.success("Report retrieved", ReportResponse.from(report, citations)));
    }

    @PostMapping("/runs/{runId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelRun(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID runId) {
        User user = getUser(userId);
        ResearchRun run = securityService.validateAndGetRun(runId, user);

        if (run.getStatus() != RunStatus.COMPLETED && run.getStatus() != RunStatus.FAILED) {
            run.setStatus(RunStatus.CANCELLED);
            run.setErrorMessage("Cancelled by user.");
            researchRunRepository.save(run);
            sseEmitterService.publishEvent(runId, "run.failed", "Research run cancelled by user.");
        }

        return ResponseEntity.ok(ApiResponse.success("Cancellation requested for run: " + runId, null));
    }

    @GetMapping(value = "/runs/{runId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID runId) {
        User user = getUser(userId);
        securityService.validateAndGetRun(runId, user);
        return sseEmitterService.subscribe(runId);
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
