package com.example.backend.research.service;

import com.example.backend.llm.LlmProvider;
import com.example.backend.llm.LlmResponse;
import com.example.backend.llm.dto.VerificationResultDto;
import com.example.backend.research.entity.Claim;
import com.example.backend.research.entity.Source;
import com.example.backend.research.enums.ClaimStatus;
import com.example.backend.research.enums.SourceType;
import com.example.backend.research.repository.ClaimEvidenceRepository;
import com.example.backend.research.repository.ClaimRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class ClaimVerificationServiceTest {

    private LlmProvider llmProvider;
    private ClaimRepository claimRepository;
    private ClaimEvidenceRepository claimEvidenceRepository;
    private ClaimVerificationService verificationService;

    @BeforeEach
    void setUp() {
        llmProvider = Mockito.mock(LlmProvider.class);
        claimRepository = Mockito.mock(ClaimRepository.class);
        claimEvidenceRepository = Mockito.mock(ClaimEvidenceRepository.class);
        verificationService = new ClaimVerificationService(llmProvider, claimRepository, claimEvidenceRepository);

        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testClaimVerificationClassification() {
        Claim claim1 = new Claim(null, "India allocated 10 billion dollars in semiconductor incentives.", "Policy");
        Claim claim2 = new Claim(null, "Domestic fabrication satisfies all consumer electronics demands.", "Market");

        VerificationResultDto.VerifiedClaimItem item1 = new VerificationResultDto.VerifiedClaimItem(
                claim1.getClaimText(),
                ClaimStatus.SUPPORTED,
                "Confirmed by national budget documents."
        );
        VerificationResultDto.VerifiedClaimItem item2 = new VerificationResultDto.VerifiedClaimItem(
                claim2.getClaimText(),
                ClaimStatus.CONTRADICTED,
                "Trade data confirms over 80% of silicon remains imported."
        );
        item2.getContradictingPassages().add("Customs import data indicates reliance on foreign foundries.");

        VerificationResultDto dto = new VerificationResultDto(List.of(item1, item2));
        when(llmProvider.generateStructured(any(), any(), eq(VerificationResultDto.class)))
                .thenReturn(LlmResponse.of(dto));

        Source source = new Source("https://example.com/data", "Source", "example.com", SourceType.WEB, "Snippet");
        List<Claim> verified = verificationService.verifyClaims(List.of(claim1, claim2), "Context text", List.of(source));

        assertEquals(2, verified.size());
        assertEquals(ClaimStatus.SUPPORTED, claim1.getVerificationStatus());
        assertEquals(ClaimStatus.CONTRADICTED, claim2.getVerificationStatus());
        assertTrue(claim2.getVerificationExplanation().contains("imported"));
    }
}
