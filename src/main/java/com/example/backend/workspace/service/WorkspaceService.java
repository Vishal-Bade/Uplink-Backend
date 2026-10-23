package com.example.backend.workspace.service;

import com.example.backend.common.exception.ResourceNotFoundException;
import com.example.backend.research.enums.WorkspaceRole;
import com.example.backend.security.ProjectSecurityService;
import com.example.backend.user.entity.User;
import com.example.backend.user.repository.UserRepository;
import com.example.backend.workspace.entity.Workspace;
import com.example.backend.workspace.entity.WorkspaceMember;
import com.example.backend.workspace.repository.WorkspaceMemberRepository;
import com.example.backend.workspace.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final ProjectSecurityService securityService;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            UserRepository userRepository,
            ProjectSecurityService securityService) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
        this.securityService = securityService;
    }

    @Transactional
    public Workspace createWorkspace(String name, String description, User owner) {
        Workspace ws = new Workspace(name, description, owner);
        ws = workspaceRepository.save(ws);

        WorkspaceMember member = new WorkspaceMember(ws, owner, WorkspaceRole.OWNER);
        workspaceMemberRepository.save(member);

        return ws;
    }

    public List<Workspace> listWorkspacesForUser(User user) {
        return workspaceRepository.findAllForUser(user.getId());
    }

    public Workspace getWorkspace(UUID workspaceId, User user) {
        securityService.validateWorkspaceAccess(workspaceId, user);
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + workspaceId));
    }

    @Transactional
    public WorkspaceMember addMember(UUID workspaceId, UUID targetUserId, WorkspaceRole role, User actor) {
        securityService.validateWorkspaceAccess(workspaceId, actor);
        Workspace ws = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .orElseGet(() -> workspaceMemberRepository.save(new WorkspaceMember(ws, targetUser, role)));
    }
}
