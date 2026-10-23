package com.example.backend.research.service;

import com.example.backend.research.entity.ResearchRun;
import com.example.backend.research.entity.RunSource;
import com.example.backend.research.entity.Source;
import com.example.backend.research.enums.SourceType;
import com.example.backend.research.repository.RunSourceRepository;
import com.example.backend.research.repository.SourceRepository;
import com.example.backend.search.SearchProvider;
import com.example.backend.search.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AcademicSearchService {

    private static final Logger log = LoggerFactory.getLogger(AcademicSearchService.class);
    private final SearchProvider academicSearchProvider;
    private final SourceRepository sourceRepository;
    private final RunSourceRepository runSourceRepository;

    public AcademicSearchService(
            @Qualifier("academicSearchProvider") SearchProvider academicSearchProvider,
            SourceRepository sourceRepository,
            RunSourceRepository runSourceRepository) {
        this.academicSearchProvider = academicSearchProvider;
        this.sourceRepository = sourceRepository;
        this.runSourceRepository = runSourceRepository;
    }

    public List<Source> searchAcademicSources(ResearchRun run, List<String> queries, int maxSources) {
        if (!run.isAcademicEnabled()) {
            return List.of();
        }

        log.info("Searching academic sources for run {} with limit: {}", run.getId(), maxSources);
        Set<String> processedUrls = new HashSet<>();
        List<Source> collectedSources = new ArrayList<>();

        for (String query : queries) {
            if (collectedSources.size() >= maxSources) break;

            try {
                List<SearchResult> papers = academicSearchProvider.search(query, 5);
                for (SearchResult paper : papers) {
                    if (collectedSources.size() >= maxSources) break;

                    String url = paper.url();
                    if (url == null || processedUrls.contains(url)) continue;
                    processedUrls.add(url);

                    Source source = sourceRepository.findByUrl(url).orElseGet(() -> {
                        Source s = new Source();
                        s.setUrl(url);
                        s.setTitle(paper.title());
                        s.setDomain(paper.domain());
                        s.setSourceType(SourceType.ACADEMIC);
                        s.setAuthors(paper.authors());
                        s.setPublishedDate(paper.publishedDate());
                        s.setSnippet(paper.snippet());
                        return sourceRepository.save(s);
                    });

                    if (!runSourceRepository.findByRunIdAndSourceId(run.getId(), source.getId()).isPresent()) {
                        RunSource runSource = new RunSource(run, source, 1.0, "Academic paper for query: " + query);
                        runSourceRepository.save(runSource);
                    }
                    collectedSources.add(source);
                }
            } catch (Exception e) {
                log.warn("Academic search query failed for '{}': {}", query, e.getMessage());
            }
        }

        log.info("Collected {} academic sources for run {}", collectedSources.size(), run.getId());
        return collectedSources;
    }
}
