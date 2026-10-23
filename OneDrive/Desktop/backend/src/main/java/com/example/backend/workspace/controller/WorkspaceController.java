package com.example.backend.workspace.controller;

import com.example.backend.common.exception.ResourceNotFoundException;
import com.example.backend.common.response.ApiResponse;
import com.example.backend.user.entity.User;
import com.example.backend.user.repository.UserRepository;
import com.example.backend.workspace.dto.CreateWorkspaceRequest;
import com.example.backend.workspace.dto.WorkspaceResponse;
import com.example.backend.workspace.entity.Workspace;
import com.example.backend.workspace.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final UserRepository userRepository;

    public WorkspaceController(WorkspaceService workspaceService, UserRepository userRepository) {
        this.workspaceService = workspaceService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WorkspaceResponse>> createWorkspace(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateWorkspaceRequest request) {
        User user = getUser(userId);
        Workspace workspace = workspaceService.createWorkspace(request.getName(), request.getDescription(), user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Workspace created successfully", WorkspaceResponse.from(workspace)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkspaceResponse>>> listWorkspaces(
            @AuthenticationPrincipal UUID userId) {
        User user = getUser(userId);
        List<WorkspaceResponse> list = workspaceService.listWorkspacesForUser(user).stream()
                .map(WorkspaceResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Workspaces retrieved successfully", list));
    }

    @GetMapping("/{workspaceId}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> getWorkspace(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID workspaceId) {
        User user = getUser(userId);
        Workspace workspace = workspaceService.getWorkspace(workspaceId, user);
        return ResponseEntity.ok(ApiResponse.success("Workspace retrieved successfully", WorkspaceResponse.from(workspace)));
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
