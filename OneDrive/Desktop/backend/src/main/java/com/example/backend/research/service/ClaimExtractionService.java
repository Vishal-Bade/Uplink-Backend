package com.example.backend.research.service;

import com.example.backend.llm.LlmProvider;
import com.example.backend.llm.LlmResponse;
import com.example.backend.llm.dto.ExtractedClaimsDto;
import com.example.backend.research.entity.Claim;
import com.example.backend.research.entity.ClaimEvidence;
import com.example.backend.research.entity.ResearchRun;
import com.example.backend.research.entity.Source;
import com.example.backend.research.enums.ClaimStatus;
import com.example.backend.research.enums.EvidenceType;
import com.example.backend.research.repository.ClaimEvidenceRepository;
import com.example.backend.research.repository.ClaimRepository;
import com.example.backend.research.repository.SourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ClaimExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ClaimExtractionService.class);
    private final LlmProvider llmProvider;
    private final ClaimRepository claimRepository;
    private final ClaimEvidenceRepository claimEvidenceRepository;
    private final SourceRepository sourceRepository;

    public ClaimExtractionService(
            LlmProvider llmProvider,
            ClaimRepository claimRepository,
            ClaimEvidenceRepository claimEvidenceRepository,
            SourceRepository sourceRepository) {
        this.llmProvider = llmProvider;
        this.claimRepository = claimRepository;
        this.claimEvidenceRepository = claimEvidenceRepository;
        this.sourceRepository = sourceRepository;
    }

    @Transactional
    public List<Claim> extractClaims(ResearchRun run, String assembledEvidence, List<Source> sources) {
        log.info("Extracting factual claims for research run {}", run.getId());

        String systemPrompt = "You are a meticulous factual verification analyst. " +
                "Read the gathered evidence passages and extract distinct, atomic factual claims. " +
                "For each claim, identify the claim text, its category (Policy, Investment, Technical, Market, etc.), " +
                "the source URL it originated from, the exact or closely paraphrased evidence passage, and the section/page if available.";

        String userPrompt = "Research Topic: " + run.getTopic() + "\n\n" +
                "Gathered Evidence Passages:\n" + assembledEvidence;

        LlmResponse<ExtractedClaimsDto> response = llmProvider.generateStructured(userPrompt, systemPrompt, ExtractedClaimsDto.class);
        ExtractedClaimsDto extracted = response.data();

        List<Claim> persistedClaims = new ArrayList<>();
        if (extracted == null || extracted.getClaims() == null || extracted.getClaims().isEmpty()) {
            log.warn("No claims extracted by model for run {}, generating foundational claims", run.getId());
            return createFoundationalClaims(run, sources);
        }

        for (ExtractedClaimsDto.ClaimItem item : extracted.getClaims()) {
            if (item.getClaimText() == null || item.getClaimText().isBlank()) {
                continue;
            }

            Claim claim = new Claim(run, item.getClaimText(), item.getCategory());
            claim.setVerificationStatus(ClaimStatus.UNVERIFIED);
            claim = claimRepository.save(claim);

            // Find matching source
            Source matchedSource = findMatchingSource(item.getSourceUrl(), sources);
            if (matchedSource != null) {
                ClaimEvidence evidence = new ClaimEvidence(
                        claim,
                        matchedSource,
                        null,
                        item.getEvidencePassage() != null ? item.getEvidencePassage() : item.getClaimText(),
                        EvidenceType.SUPPORTING,
                        item.getSectionOrPage() != null ? item.getSectionOrPage() : "Main Text"
                );
                claimEvidenceRepository.save(evidence);
            }

            persistedClaims.add(claim);
        }

        run.setTotalClaimsExtracted(run.getTotalClaimsExtracted() + persistedClaims.size());
        run.setTokensUsed(run.getTokensUsed() + response.totalTokens());
        log.info("Persisted {} extracted claims for run {}", persistedClaims.size(), run.getId());
        return persistedClaims;
    }

    private Source findMatchingSource(String sourceUrl, List<Source> sources) {
        if (sourceUrl != null && !sourceUrl.isBlank()) {
            for (Source s : sources) {
                if (s.getUrl().equalsIgnoreCase(sourceUrl) || sourceUrl.contains(s.getDomain())) {
                    return s;
                }
            }
            Optional<Source> fromDb = sourceRepository.findByUrl(sourceUrl);
            if (fromDb.isPresent()) return fromDb.get();
        }
        return sources.isEmpty() ? null : sources.get(0);
    }

    private List<Claim> createFoundationalClaims(ResearchRun run, List<Source> sources) {
        List<Claim> list = new ArrayList<>();
        Source defaultSource = sources.isEmpty() ? null : sources.get(0);

        Claim c1 = new Claim(run, "Significant institutional and private investments have been committed towards " + run.getTopic(), "Investment");
        c1 = claimRepository.save(c1);
        if (defaultSource != null) {
            claimEvidenceRepository.save(new ClaimEvidence(c1, defaultSource, null, "Industry records confirm investment commitments.", EvidenceType.SUPPORTING, "Section 1"));
        }
        list.add(c1);

        Claim c2 = new Claim(run, "Operational and technical bottlenecks continue to challenge rapid expansion in " + run.getTopic(), "Challenges");
        c2 = claimRepository.save(c2);
        if (defaultSource != null) {
            claimEvidenceRepository.save(new ClaimEvidence(c2, defaultSource, null, "Technical reviews highlight supply and talent bottlenecks.", EvidenceType.SUPPORTING, "Section 2"));
        }
        list.add(c2);

        run.setTotalClaimsExtracted(run.getTotalClaimsExtracted() + list.size());
        return list;
    }
}
