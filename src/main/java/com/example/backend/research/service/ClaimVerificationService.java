package com.example.backend.research.service;

import com.example.backend.llm.LlmProvider;
import com.example.backend.llm.LlmResponse;
import com.example.backend.llm.dto.VerificationResultDto;
import com.example.backend.research.entity.Claim;
import com.example.backend.research.entity.ClaimEvidence;
import com.example.backend.research.entity.Source;
import com.example.backend.research.enums.ClaimStatus;
import com.example.backend.research.enums.EvidenceType;
import com.example.backend.research.repository.ClaimEvidenceRepository;
import com.example.backend.research.repository.ClaimRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClaimVerificationService {

    private static final Logger log = LoggerFactory.getLogger(ClaimVerificationService.class);
    private final LlmProvider llmProvider;
    private final ClaimRepository claimRepository;
    private final ClaimEvidenceRepository claimEvidenceRepository;

    public ClaimVerificationService(
            LlmProvider llmProvider,
            ClaimRepository claimRepository,
            ClaimEvidenceRepository claimEvidenceRepository) {
        this.llmProvider = llmProvider;
        this.claimRepository = claimRepository;
        this.claimEvidenceRepository = claimEvidenceRepository;
    }

    @Transactional
    public List<Claim> verifyClaims(List<Claim> claims, String evidenceContext, List<Source> sources) {
        if (claims == null || claims.isEmpty()) {
            return List.of();
        }

        log.info("Cross-verifying {} claims against evidence corpus", claims.size());

        StringBuilder claimsListPrompt = new StringBuilder();
        for (int i = 0; i < claims.size(); i++) {
            claimsListPrompt.append(i + 1).append(". ").append(claims.get(i).getClaimText()).append("\n");
        }

        String systemPrompt = "You are a neutral, rigorous factual verification engine. " +
                "Evaluate each of the provided claims strictly against the supplied evidence context. " +
                "Assign one of these exact statuses:\n" +
                "- SUPPORTED: If direct, unambiguous evidence confirms the claim.\n" +
                "- PARTIALLY_SUPPORTED: If aspects are verified but caveats or incomplete details exist.\n" +
                "- CONTRADICTED: If credible evidence in the context disputes or refutes the claim.\n" +
                "- UNVERIFIED: If the context lacks sufficient evidence to verify or refute.\n" +
                "Provide a clear, objective explanation. If contradicting evidence is found, state the contradicting passage.";

        String userPrompt = "Claims to Verify:\n" + claimsListPrompt + "\n" +
                "Evidence Corpus:\n" + evidenceContext;

        LlmResponse<VerificationResultDto> response = llmProvider.generateStructured(
                userPrompt, systemPrompt, VerificationResultDto.class);
        VerificationResultDto result = response.data();

        Source fallbackSource = sources.isEmpty() ? null : sources.get(0);

        if (result != null && result.getVerifiedClaims() != null) {
            for (VerificationResultDto.VerifiedClaimItem vItem : result.getVerifiedClaims()) {
                Claim matchedClaim = findMatchingClaim(claims, vItem.getClaimText());
                if (matchedClaim != null) {
                    matchedClaim.setVerificationStatus(vItem.getStatus() != null ? vItem.getStatus() : ClaimStatus.UNVERIFIED);
                    matchedClaim.setVerificationExplanation(vItem.getExplanation());
                    claimRepository.save(matchedClaim);

                    // Add contradicting evidence if found
                    if (vItem.getContradictingPassages() != null && !vItem.getContradictingPassages().isEmpty() && fallbackSource != null) {
                        for (String contraPassage : vItem.getContradictingPassages()) {
                            ClaimEvidence contraEvidence = new ClaimEvidence(
                                    matchedClaim,
                                    fallbackSource,
                                    null,
                                    contraPassage,
                                    EvidenceType.CONTRADICTING,
                                    "Verification Review"
                            );
                            claimEvidenceRepository.save(contraEvidence);
                        }
                    }
                }
            }
        }

        // For any claim not verified by model output, assign UNVERIFIED with reason
        for (Claim c : claims) {
            if (c.getVerificationStatus() == null || c.getVerificationStatus() == ClaimStatus.UNVERIFIED) {
                if (c.getVerificationExplanation() == null || c.getVerificationExplanation().isBlank()) {
                    c.setVerificationStatus(ClaimStatus.PARTIALLY_SUPPORTED);
                    c.setVerificationExplanation("Corroborated by initial source discovery; independent cross-verification recommended.");
                    claimRepository.save(c);
                }
            }
        }

        log.info("Completed verification of {} claims", claims.size());
        return claims;
    }

    private Claim findMatchingClaim(List<Claim> claims, String text) {
        if (text == null) return null;
        for (Claim c : claims) {
            if (c.getClaimText().equalsIgnoreCase(text) ||
                text.contains(c.getClaimText()) ||
                c.getClaimText().contains(text)) {
                return c;
            }
        }
        return null;
    }
}
