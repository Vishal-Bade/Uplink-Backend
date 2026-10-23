package com.example.backend.project.service;

import com.example.backend.project.entity.Project;
import com.example.backend.project.repository.ProjectRepository;
import com.example.backend.research.enums.ProjectStatus;
import com.example.backend.security.ProjectSecurityService;
import com.example.backend.user.entity.User;
import com.example.backend.workspace.entity.Workspace;
import com.example.backend.workspace.service.WorkspaceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final WorkspaceService workspaceService;
    private final ProjectSecurityService securityService;

    public ProjectService(
            ProjectRepository projectRepository,
            WorkspaceService workspaceService,
            ProjectSecurityService securityService) {
        this.projectRepository = projectRepository;
        this.workspaceService = workspaceService;
        this.securityService = securityService;
    }

    @Transactional
    public Project createProject(UUID workspaceId, String name, String description, String objectives, User user) {
        Workspace workspace = workspaceService.getWorkspace(workspaceId, user);
        Project project = new Project(workspace, name, description, objectives, user);
        return projectRepository.save(project);
    }

    public Page<Project> listProjects(UUID workspaceId, Pageable pageable, User user) {
        if (workspaceId != null) {
            securityService.validateWorkspaceAccess(workspaceId, user);
            return projectRepository.findByWorkspaceId(workspaceId, pageable);
        }
        return projectRepository.findAllAccessibleProjects(user.getId(), pageable);
    }

    public Project getProject(UUID projectId, User user) {
        return securityService.validateAndGetProject(projectId, user);
    }

    @Transactional
    public Project updateProject(UUID projectId, String name, String description, String objectives, ProjectStatus status, User user) {
        Project project = securityService.validateAndGetProject(projectId, user);

        if (name != null && !name.isBlank()) project.setName(name);
        if (description != null) project.setDescription(description);
        if (objectives != null) project.setResearchObjectives(objectives);
        if (status != null) project.setStatus(status);

        return projectRepository.save(project);
    }
}
