package com.example.backend.project.repository;

import com.example.backend.project.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByWorkspaceId(UUID workspaceId);
    Page<Project> findByWorkspaceId(UUID workspaceId, Pageable pageable);

    @Query("SELECT p FROM Project p " +
           "JOIN p.workspace w " +
           "LEFT JOIN WorkspaceMember wm ON wm.workspace = w " +
           "WHERE (w.owner.id = :userId OR wm.user.id = :userId OR p.createdBy.id = :userId)")
    Page<Project> findAllAccessibleProjects(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT p FROM Project p " +
           "JOIN p.workspace w " +
           "LEFT JOIN WorkspaceMember wm ON wm.workspace = w " +
           "WHERE p.id = :projectId AND (w.owner.id = :userId OR wm.user.id = :userId OR p.createdBy.id = :userId)")
    Optional<Project> findAccessibleById(@Param("projectId") UUID projectId, @Param("userId") UUID userId);
}
