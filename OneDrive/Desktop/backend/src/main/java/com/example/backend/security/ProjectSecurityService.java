package com.example.backend.security;

import com.example.backend.common.exception.ResourceNotFoundException;
import com.example.backend.project.entity.Project;
import com.example.backend.project.repository.ProjectRepository;
import com.example.backend.research.entity.ResearchRun;
import com.example.backend.research.repository.ResearchRunRepository;
import com.example.backend.user.entity.User;
import com.example.backend.workspace.entity.Workspace;
import com.example.backend.workspace.repository.WorkspaceMemberRepository;
import com.example.backend.workspace.repository.WorkspaceRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProjectSecurityService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final ProjectRepository projectRepository;
    private final ResearchRunRepository researchRunRepository;

    public ProjectSecurityService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            ProjectRepository projectRepository,
            ResearchRunRepository researchRunRepository) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.projectRepository = projectRepository;
        this.researchRunRepository = researchRunRepository;
    }

    public void validateWorkspaceAccess(UUID workspaceId, User user) {
        Workspace ws = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + workspaceId));

        if (ws.getOwner().getId().equals(user.getId())) {
            return;
        }

        boolean isMember = workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, user.getId());
        if (!isMember) {
            throw new AccessDeniedException("User does not have access to this workspace");
        }
    }

    public Project validateAndGetProject(UUID projectId, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        if (project.getCreatedBy().getId().equals(user.getId())) {
            return project;
        }

        validateWorkspaceAccess(project.getWorkspace().getId(), user);
        return project;
    }

    public ResearchRun validateAndGetRun(UUID runId, User user) {
        ResearchRun run = researchRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Research run not found: " + runId));

        if (run.getCreatedBy().getId().equals(user.getId())) {
            return run;
        }

        validateWorkspaceAccess(run.getProject().getWorkspace().getId(), user);
        return run;
    }
}
