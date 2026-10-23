package com.example.backend.llm.dto;

import java.util.ArrayList;
import java.util.List;

public class ExtractedClaimsDto {
    private List<ClaimItem> claims = new ArrayList<>();

    public ExtractedClaimsDto() {}

    public ExtractedClaimsDto(List<ClaimItem> claims) {
        this.claims = claims;
    }

    public static class ClaimItem {
        private String claimText;
        private String category;
        private String sourceUrl;
        private String evidencePassage;
        private String sectionOrPage;

        public ClaimItem() {}

        public ClaimItem(String claimText, String category, String sourceUrl, String evidencePassage, String sectionOrPage) {
            this.claimText = claimText;
            this.category = category;
            this.sourceUrl = sourceUrl;
            this.evidencePassage = evidencePassage;
            this.sectionOrPage = sectionOrPage;
        }

        public String getClaimText() { return claimText; }
        public void setClaimText(String claimText) { this.claimText = claimText; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getSourceUrl() { return sourceUrl; }
        public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

        public String getEvidencePassage() { return evidencePassage; }
        public void setEvidencePassage(String evidencePassage) { this.evidencePassage = evidencePassage; }

        public String getSectionOrPage() { return sectionOrPage; }
        public void setSectionOrPage(String sectionOrPage) { this.sectionOrPage = sectionOrPage; }
    }

    public List<ClaimItem> getClaims() { return claims; }
    public void setClaims(List<ClaimItem> claims) { this.claims = claims; }
}
