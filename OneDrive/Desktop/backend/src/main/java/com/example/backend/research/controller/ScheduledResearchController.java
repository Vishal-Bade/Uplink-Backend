package com.example.backend.research.controller;

import com.example.backend.common.exception.ResourceNotFoundException;
import com.example.backend.common.response.ApiResponse;
import com.example.backend.research.entity.ScheduledResearch;
import com.example.backend.research.enums.ResearchDepth;
import com.example.backend.research.service.ScheduledResearchService;
import com.example.backend.security.ProjectSecurityService;
import com.example.backend.user.entity.User;
import com.example.backend.user.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ScheduledResearchController {

    private final ScheduledResearchService scheduledResearchService;
    private final ProjectSecurityService securityService;
    private final UserRepository userRepository;

    public record CreateScheduleRequest(
            @NotBlank(message = "Topic is required") String topic,
            @NotBlank(message = "Cron expression is required") String cronExpression,
            ResearchDepth depth
    ) {}

    public record UpdateScheduleRequest(
            Boolean active,
            String topic,
            String cronExpression,
            ResearchDepth depth
    ) {}

    public record ScheduleResponse(
            UUID id,
            UUID projectId,
            String topic,
            String cronExpression,
            boolean active,
            ResearchDepth depth,
            LocalDateTime lastRunAt,
            LocalDateTime nextRunAt,
            LocalDateTime createdAt
    ) {
        public static ScheduleResponse from(ScheduledResearch s) {
            return new ScheduleResponse(
                    s.getId(),
                    s.getProject() != null ? s.getProject().getId() : null,
                    s.getTopic(),
                    s.getCronExpression(),
                    s.isActive(),
                    s.getDepth(),
                    s.getLastRunAt(),
                    s.getNextRunAt(),
                    s.getCreatedAt()
            );
        }
    }

    public ScheduledResearchController(
            ScheduledResearchService scheduledResearchService,
            ProjectSecurityService securityService,
            UserRepository userRepository) {
        this.scheduledResearchService = scheduledResearchService;
        this.securityService = securityService;
        this.userRepository = userRepository;
    }

    @PostMapping("/projects/{projectId}/schedules")
    public ResponseEntity<ApiResponse<ScheduleResponse>> createSchedule(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID projectId,
            @RequestBody CreateScheduleRequest request) {
        User user = getUser(userId);
        securityService.validateAndGetProject(projectId, user);

        ScheduledResearch schedule = scheduledResearchService.createSchedule(
                projectId,
                request.topic(),
                request.cronExpression(),
                request.depth() != null ? request.depth() : ResearchDepth.STANDARD,
                user
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Scheduled research created", ScheduleResponse.from(schedule)));
    }

    @GetMapping("/projects/{projectId}/schedules")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> listSchedules(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID projectId) {
        User user = getUser(userId);
        securityService.validateAndGetProject(projectId, user);

        List<ScheduleResponse> list = scheduledResearchService.listByProject(projectId).stream()
                .map(ScheduleResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Schedules retrieved", list));
    }

    @PatchMapping("/schedules/{scheduleId}")
    public ResponseEntity<ApiResponse<ScheduleResponse>> updateSchedule(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID scheduleId,
            @RequestBody UpdateScheduleRequest request) {
        ScheduledResearch schedule = scheduledResearchService.updateSchedule(
                scheduleId,
                request.active(),
                request.topic(),
                request.cronExpression(),
                request.depth()
        );
        return ResponseEntity.ok(ApiResponse.success("Schedule updated", ScheduleResponse.from(schedule)));
    }

    @DeleteMapping("/schedules/{scheduleId}")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID scheduleId) {
        scheduledResearchService.deleteSchedule(scheduleId);
        return ResponseEntity.ok(ApiResponse.success("Schedule deleted successfully", null));
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
