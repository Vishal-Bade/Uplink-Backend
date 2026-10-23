package com.example.backend.research.controller;

import com.example.backend.common.exception.ResourceNotFoundException;
import com.example.backend.common.response.ApiResponse;
import com.example.backend.research.entity.Artifact;
import com.example.backend.research.entity.Report;
import com.example.backend.research.entity.ResearchRun;
import com.example.backend.research.enums.ArtifactType;
import com.example.backend.research.repository.ArtifactRepository;
import com.example.backend.research.repository.ReportRepository;
import com.example.backend.research.service.ReportExportService;
import com.example.backend.security.ProjectSecurityService;
import com.example.backend.user.entity.User;
import com.example.backend.user.repository.UserRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ArtifactController {

    private final ArtifactRepository artifactRepository;
    private final ReportRepository reportRepository;
    private final ReportExportService reportExportService;
    private final ProjectSecurityService securityService;
    private final UserRepository userRepository;

    public record ArtifactDto(
            UUID id,
            UUID runId,
            ArtifactType artifactType,
            String fileName,
            long fileSize,
            String mimeType
    ) {
        public static ArtifactDto from(Artifact a) {
            return new ArtifactDto(
                    a.getId(),
                    a.getRun() != null ? a.getRun().getId() : null,
                    a.getArtifactType(),
                    a.getFileName(),
                    a.getFileSize(),
                    a.getMimeType()
            );
        }
    }

    public ArtifactController(
            ArtifactRepository artifactRepository,
            ReportRepository reportRepository,
            ReportExportService reportExportService,
            ProjectSecurityService securityService,
            UserRepository userRepository) {
        this.artifactRepository = artifactRepository;
        this.reportRepository = reportRepository;
        this.reportExportService = reportExportService;
        this.securityService = securityService;
        this.userRepository = userRepository;
    }

    @PostMapping("/runs/{runId}/exports")
    public ResponseEntity<ApiResponse<List<ArtifactDto>>> triggerExport(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID runId) throws IOException {
        User user = getUser(userId);
        ResearchRun run = securityService.validateAndGetRun(runId, user);

        Report report = reportRepository.findByRunId(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not yet generated for run: " + runId));

        List<ArtifactDto> exported = new ArrayList<>();
        Artifact pdf = reportExportService.exportPdf(report);
        exported.add(ArtifactDto.from(pdf));

        Artifact pptx = reportExportService.exportPptx(report);
        exported.add(ArtifactDto.from(pptx));

        return ResponseEntity.ok(ApiResponse.success("Artifacts exported successfully", exported));
    }

    @GetMapping("/artifacts/{artifactId}")
    public ResponseEntity<Resource> downloadArtifact(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID artifactId) {
        User user = getUser(userId);
        Artifact artifact = artifactRepository.findById(artifactId)
                .orElseThrow(() -> new ResourceNotFoundException("Artifact not found: " + artifactId));

        // Enforce authorization
        securityService.validateAndGetRun(artifact.getRun().getId(), user);

        File file = new File(artifact.getStoragePath());
        if (!file.exists()) {
            throw new ResourceNotFoundException("File artifact is missing on server: " + artifact.getFileName());
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(artifact.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + artifact.getFileName() + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(file.length()))
                .body(resource);
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
