package com.example.backend.workspace.repository;

import com.example.backend.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
    List<Workspace> findByOwnerId(UUID ownerId);

    @Query("SELECT w FROM Workspace w LEFT JOIN WorkspaceMember wm ON wm.workspace = w WHERE w.owner.id = :userId OR wm.user.id = :userId")
    List<Workspace> findAllForUser(@Param("userId") UUID userId);
}
