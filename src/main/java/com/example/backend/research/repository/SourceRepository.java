package com.example.backend.research.repository;

import com.example.backend.research.entity.Source;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SourceRepository extends JpaRepository<Source, UUID> {
    Optional<Source> findByUrl(String url);
    Optional<Source> findByContentHash(String contentHash);
    boolean existsByUrl(String url);
}
