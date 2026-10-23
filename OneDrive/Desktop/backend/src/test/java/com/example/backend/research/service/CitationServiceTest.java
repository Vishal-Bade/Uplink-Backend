package com.example.backend.research.service;

import com.example.backend.research.entity.Citation;
import com.example.backend.research.entity.Report;
import com.example.backend.research.entity.Source;
import com.example.backend.research.enums.SourceType;
import com.example.backend.research.repository.CitationRepository;
import com.example.backend.research.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class CitationServiceTest {

    private CitationRepository citationRepository;
    private ReportRepository reportRepository;
    private CitationService citationService;

    @BeforeEach
    void setUp() {
        citationRepository = Mockito.mock(CitationRepository.class);
        reportRepository = Mockito.mock(ReportRepository.class);
        citationService = new CitationService(citationRepository, reportRepository);
        when(citationRepository.save(any(Citation.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testCitationFormatting() {
        Report report = new Report();
        report.setTitle("Semiconductor Ecosystem Report");

        Source s1 = new Source("https://example.com/policy", "National Policy Brief", "example.com", SourceType.WEB, "Snippet text");
        s1.setAuthors("A. Sharma, B. Patel");
        s1.setPublishedDate("2024");

        List<Citation> citations = citationService.generateAndPersistCitations(report, List.of(s1));

        assertEquals(1, citations.size());
        Citation c1 = citations.get(0);
        assertEquals(1, c1.getCitationNumber());
        assertTrue(c1.getFormattedIeee().startsWith("[1]"));
        assertTrue(c1.getFormattedIeee().contains("https://example.com/policy"));
        assertTrue(c1.getFormattedIeee().contains("National Policy Brief"));

        assertTrue(c1.getFormattedApa().contains("A. Sharma, B. Patel"));
        assertTrue(c1.getFormattedApa().contains("(2024)"));
    }
}
