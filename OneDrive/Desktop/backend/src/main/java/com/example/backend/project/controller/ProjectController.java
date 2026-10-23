package com.example.backend.project.controller;

import com.example.backend.common.exception.ResourceNotFoundException;
import com.example.backend.common.response.ApiResponse;
import com.example.backend.project.dto.CreateProjectRequest;
import com.example.backend.project.dto.ProjectResponse;
import com.example.backend.project.dto.UpdateProjectRequest;
import com.example.backend.project.entity.Project;
import com.example.backend.project.service.ProjectService;
import com.example.backend.user.entity.User;
import com.example.backend.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final UserRepository userRepository;

    public ProjectController(ProjectService projectService, UserRepository userRepository) {
        this.projectService = projectService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateProjectRequest request) {
        User user = getUser(userId);
        Project project = projectService.createProject(
                request.getWorkspaceId(),
                request.getName(),
                request.getDescription(),
                request.getResearchObjectives(),
                user
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Project created successfully", ProjectResponse.from(project)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProjectResponse>>> listProjects(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(required = false) UUID workspaceId,
            @PageableDefault(size = 20) Pageable pageable) {
        User user = getUser(userId);
        Page<ProjectResponse> page = projectService.listProjects(workspaceId, pageable, user)
                .map(ProjectResponse::from);
        return ResponseEntity.ok(ApiResponse.success("Projects retrieved successfully", page));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProject(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID projectId) {
        User user = getUser(userId);
        Project project = projectService.getProject(projectId, user);
        return ResponseEntity.ok(ApiResponse.success("Project retrieved successfully", ProjectResponse.from(project)));
    }

    @PatchMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID projectId,
            @RequestBody UpdateProjectRequest request) {
        User user = getUser(userId);
        Project project = projectService.updateProject(
                projectId,
                request.getName(),
                request.getDescription(),
                request.getResearchObjectives(),
                request.getStatus(),
                user
        );
        return ResponseEntity.ok(ApiResponse.success("Project updated successfully", ProjectResponse.from(project)));
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
