package com.example.backend.llm.dto;

import com.example.backend.research.enums.ClaimStatus;
import java.util.ArrayList;
import java.util.List;

public class VerificationResultDto {
    private List<VerifiedClaimItem> verifiedClaims = new ArrayList<>();

    public VerificationResultDto() {}

    public VerificationResultDto(List<VerifiedClaimItem> verifiedClaims) {
        this.verifiedClaims = verifiedClaims;
    }

    public static class VerifiedClaimItem {
        private String claimText;
        private ClaimStatus status;
        private String explanation;
        private List<String> supportingPassages = new ArrayList<>();
        private List<String> contradictingPassages = new ArrayList<>();

        public VerifiedClaimItem() {}

        public VerifiedClaimItem(String claimText, ClaimStatus status, String explanation) {
            this.claimText = claimText;
            this.status = status;
            this.explanation = explanation;
        }

        public String getClaimText() { return claimText; }
        public void setClaimText(String claimText) { this.claimText = claimText; }

        public ClaimStatus getStatus() { return status; }
        public void setStatus(ClaimStatus status) { this.status = status; }

        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }

        public List<String> getSupportingPassages() { return supportingPassages; }
        public void setSupportingPassages(List<String> supportingPassages) { this.supportingPassages = supportingPassages; }

        public List<String> getContradictingPassages() { return contradictingPassages; }
        public void setContradictingPassages(List<String> contradictingPassages) { this.contradictingPassages = contradictingPassages; }
    }

    public List<VerifiedClaimItem> getVerifiedClaims() { return verifiedClaims; }
    public void setVerifiedClaims(List<VerifiedClaimItem> verifiedClaims) { this.verifiedClaims = verifiedClaims; }
}
