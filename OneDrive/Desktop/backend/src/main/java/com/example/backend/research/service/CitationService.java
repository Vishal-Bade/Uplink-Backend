package com.example.backend.research.service;

import com.example.backend.research.entity.Citation;
import com.example.backend.research.entity.Report;
import com.example.backend.research.entity.Source;
import com.example.backend.research.repository.CitationRepository;
import com.example.backend.research.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class CitationService {

    private static final Logger log = LoggerFactory.getLogger(CitationService.class);
    private final CitationRepository citationRepository;
    private final ReportRepository reportRepository;

    public CitationService(CitationRepository citationRepository, ReportRepository reportRepository) {
        this.citationRepository = citationRepository;
        this.reportRepository = reportRepository;
    }

    @Transactional
    public List<Citation> generateAndPersistCitations(Report report, List<Source> sources) {
        if (sources == null || sources.isEmpty()) {
            return List.of();
        }

        log.info("Generating IEEE and APA citations for report: {}", report.getId());
        List<Citation> citations = new ArrayList<>();
        StringBuilder ieeeBlock = new StringBuilder("### References (IEEE Format)\n\n");
        StringBuilder apaBlock = new StringBuilder("\n### References (APA Format)\n\n");

        int citationNum = 1;
        String currentYear = String.valueOf(LocalDate.now().getYear());

        for (Source source : sources) {
            String citationKey = "ref-" + citationNum;
            String authors = (source.getAuthors() != null && !source.getAuthors().isBlank()) ? source.getAuthors() : "Staff";
            String title = (source.getTitle() != null && !source.getTitle().isBlank()) ? source.getTitle() : "Online Document";
            String year = (source.getPublishedDate() != null && !source.getPublishedDate().isBlank()) ? source.getPublishedDate() : currentYear;
            String domain = (source.getDomain() != null && !source.getDomain().isBlank()) ? source.getDomain() : "Web Source";
            String url = source.getUrl();

            // IEEE Format: [1] J. K. Author, "Title of document," Domain, Year. [Online]. Available: URL.
            String formattedIeee = String.format("[%d] %s, \"%s,\" %s, %s. [Online]. Available: %s",
                    citationNum, authors, title, domain, year, url);

            // APA Format: Author, A. (Year). Title of document. Domain. URL
            String formattedApa = String.format("%s (%s). %s. %s. %s",
                    authors, year, title, domain, url);

            Citation citation = new Citation(report, source, citationNum, citationKey, formattedIeee, formattedApa);
            citations.add(citationRepository.save(citation));

            ieeeBlock.append(formattedIeee).append("\n\n");
            apaBlock.append(citationNum).append(". ").append(formattedApa).append("\n\n");
            citationNum++;
        }

        report.setCitationsFormatted(ieeeBlock.toString() + apaBlock.toString());
        reportRepository.save(report);

        log.info("Successfully persisted {} citations for report {}", citations.size(), report.getId());
        return citations;
    }
}
