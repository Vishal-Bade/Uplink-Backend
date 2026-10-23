package com.example.backend.research.service;

import com.example.backend.research.entity.*;
import com.example.backend.research.enums.ArtifactType;
import com.example.backend.research.repository.ArtifactRepository;
import com.example.backend.research.repository.ClaimRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xslf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportExportService {

    private static final Logger log = LoggerFactory.getLogger(ReportExportService.class);
    private final ArtifactRepository artifactRepository;
    private final ClaimRepository claimRepository;
    private final String storageDir;

    public ReportExportService(
            ArtifactRepository artifactRepository,
            ClaimRepository claimRepository,
            @Value("${artifacts.storage-dir:target/artifacts}") String storageDir) {
        this.artifactRepository = artifactRepository;
        this.claimRepository = claimRepository;
        this.storageDir = storageDir;
        initStorage();
    }

    private void initStorage() {
        try {
            Files.createDirectories(Paths.get(storageDir));
        } catch (IOException e) {
            log.error("Could not initialize storage directory {}: {}", storageDir, e.getMessage());
        }
    }

    @Transactional
    public Artifact exportPdf(Report report) throws IOException {
        ResearchRun run = report.getRun();
        List<Claim> claims = claimRepository.findByRunId(run.getId());
        String fileName = "Report_" + run.getId() + ".pdf";
        Path targetPath = Paths.get(storageDir, fileName);

        PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float margin = 50;
                float y = PDRectangle.A4.getHeight() - margin;
                float width = PDRectangle.A4.getWidth() - 2 * margin;

                // Header
                contentStream.beginText();
                contentStream.setFont(fontBold, 10);
                contentStream.setNonStrokingColor(Color.GRAY);
                contentStream.newLineAtOffset(margin, y);
                contentStream.showText("AUTONOMOUS RESEARCH WORKSPACE — VERIFIED RESEARCH REPORT");
                contentStream.endText();
                y -= 25;

                // Title
                contentStream.beginText();
                contentStream.setFont(fontBold, 16);
                contentStream.setNonStrokingColor(new Color(25, 45, 85));
                contentStream.newLineAtOffset(margin, y);
                contentStream.showText(sanitize(truncate(report.getTitle(), 70)));
                contentStream.endText();
                y -= 30;

                // Executive Summary Heading
                contentStream.beginText();
                contentStream.setFont(fontBold, 12);
                contentStream.setNonStrokingColor(Color.BLACK);
                contentStream.newLineAtOffset(margin, y);
                contentStream.showText("Executive Summary");
                contentStream.endText();
                y -= 15;

                // Executive Summary Content
                List<String> summaryLines = wrapText(report.getExecutiveSummary(), 85);
                contentStream.beginText();
                contentStream.setFont(fontRegular, 9);
                contentStream.newLineAtOffset(margin, y);
                for (String line : summaryLines) {
                    contentStream.showText(sanitize(line));
                    contentStream.newLineAtOffset(0, -12);
                    y -= 12;
                    if (y < margin + 50) break;
                }
                contentStream.endText();
                y -= 15;

                // Key Findings Heading
                if (y > margin + 100) {
                    contentStream.beginText();
                    contentStream.setFont(fontBold, 12);
                    contentStream.newLineAtOffset(margin, y);
                    contentStream.showText("Key Findings");
                    contentStream.endText();
                    y -= 15;

                    List<String> findingsLines = wrapText(report.getKeyFindings(), 85);
                    contentStream.beginText();
                    contentStream.setFont(fontRegular, 9);
                    contentStream.newLineAtOffset(margin, y);
                    for (String line : findingsLines) {
                        contentStream.showText(sanitize(line));
                        contentStream.newLineAtOffset(0, -12);
                        y -= 12;
                        if (y < margin + 50) break;
                    }
                    contentStream.endText();
                    y -= 15;
                }

                // Verified Claims Summary
                if (y > margin + 60 && !claims.isEmpty()) {
                    contentStream.beginText();
                    contentStream.setFont(fontBold, 12);
                    contentStream.newLineAtOffset(margin, y);
                    contentStream.showText("Factual Claims & Verification Statuses (" + claims.size() + " total)");
                    contentStream.endText();
                    y -= 15;

                    contentStream.beginText();
                    contentStream.setFont(fontRegular, 8);
                    contentStream.newLineAtOffset(margin, y);
                    for (int i = 0; i < Math.min(claims.size(), 5); i++) {
                        Claim c = claims.get(i);
                        String claimSummary = "[" + c.getVerificationStatus() + "] " + truncate(c.getClaimText(), 80);
                        contentStream.showText(sanitize(claimSummary));
                        contentStream.newLineAtOffset(0, -11);
                        y -= 11;
                        if (y < margin + 20) break;
                    }
                    contentStream.endText();
                }

                // Footer
                contentStream.beginText();
                contentStream.setFont(fontRegular, 8);
                contentStream.setNonStrokingColor(Color.GRAY);
                contentStream.newLineAtOffset(margin, margin / 2);
                contentStream.showText("Generated autonomously on " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + " | Page 1");
                contentStream.endText();
            }

            document.save(targetPath.toFile());
        }

        File file = targetPath.toFile();
        Artifact artifact = new Artifact(
                run,
                ArtifactType.PDF,
                fileName,
                file.length(),
                "application/pdf",
                targetPath.toAbsolutePath().toString()
        );
        log.info("Exported PDF artifact: {} (size: {} bytes)", fileName, file.length());
        return artifactRepository.save(artifact);
    }

    @Transactional
    public Artifact exportPptx(Report report) throws IOException {
        ResearchRun run = report.getRun();
        List<Claim> claims = claimRepository.findByRunId(run.getId());
        String fileName = "Presentation_" + run.getId() + ".pptx";
        Path targetPath = Paths.get(storageDir, fileName);

        try (XMLSlideShow ppt = new XMLSlideShow()) {
            // Slide 1: Title Slide
            XSLFSlide titleSlide = ppt.createSlide();
            XSLFTextBox titleBox = titleSlide.createTextBox();
            titleBox.setAnchor(new java.awt.Rectangle(50, 100, 620, 120));
            XSLFTextParagraph titlePara = titleBox.addNewTextParagraph();
            XSLFTextRun titleRun = titlePara.addNewTextRun();
            titleRun.setText(report.getTitle());
            titleRun.setFontSize(26.0);
            titleRun.setBold(true);
            titleRun.setFontColor(new Color(25, 45, 85));

            XSLFTextParagraph subPara = titleBox.addNewTextParagraph();
            XSLFTextRun subRun = subPara.addNewTextRun();
            subRun.setText("Autonomous Research Workspace Presentation — " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
            subRun.setFontSize(14.0);
            subRun.setFontColor(Color.DARK_GRAY);

            // Slide 2: Executive Summary
            XSLFSlide execSlide = ppt.createSlide();
            addSlideHeader(execSlide, "Executive Summary");
            addSlideBullet(execSlide, report.getExecutiveSummary() != null ? report.getExecutiveSummary() : "Summary of findings.");

            // Slide 3: Key Findings
            XSLFSlide findingsSlide = ppt.createSlide();
            addSlideHeader(findingsSlide, "Key Findings & Insights");
            if (report.getKeyFindings() != null) {
                String[] points = report.getKeyFindings().split("\n");
                for (String pt : points) {
                    if (!pt.isBlank()) addSlideBullet(findingsSlide, pt.trim());
                }
            }

            // Slide 4: Verified Claims
            XSLFSlide claimsSlide = ppt.createSlide();
            addSlideHeader(claimsSlide, "Claim-Evidence Verification Matrix");
            for (int i = 0; i < Math.min(claims.size(), 4); i++) {
                Claim c = claims.get(i);
                addSlideBullet(claimsSlide, "[" + c.getVerificationStatus() + "] " + c.getClaimText());
            }

            // Slide 5: Strategic Outlook & Conclusions
            XSLFSlide conclSlide = ppt.createSlide();
            addSlideHeader(conclSlide, "Strategic Outlook & Conclusions");
            addSlideBullet(conclSlide, report.getConclusions() != null ? report.getConclusions() : "Strategic conclusions grounded in evidence.");

            try (FileOutputStream out = new FileOutputStream(targetPath.toFile())) {
                ppt.write(out);
            }
        }

        File file = targetPath.toFile();
        Artifact artifact = new Artifact(
                run,
                ArtifactType.PPTX,
                fileName,
                file.length(),
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                targetPath.toAbsolutePath().toString()
        );
        log.info("Exported PPTX artifact: {} (size: {} bytes)", fileName, file.length());
        return artifactRepository.save(artifact);
    }

    private void addSlideHeader(XSLFSlide slide, String headerText) {
        XSLFTextBox headerBox = slide.createTextBox();
        headerBox.setAnchor(new java.awt.Rectangle(50, 30, 620, 50));
        XSLFTextParagraph p = headerBox.addNewTextParagraph();
        XSLFTextRun r = p.addNewTextRun();
        r.setText(headerText);
        r.setFontSize(22.0);
        r.setBold(true);
        r.setFontColor(new Color(25, 45, 85));
    }

    private void addSlideBullet(XSLFSlide slide, String text) {
        XSLFTextBox bodyBox = slide.createTextBox();
        bodyBox.setAnchor(new java.awt.Rectangle(50, 100, 620, 280));
        XSLFTextParagraph p = bodyBox.addNewTextParagraph();
        p.setBullet(true);
        XSLFTextRun r = p.addNewTextRun();
        r.setText(text);
        r.setFontSize(14.0);
        r.setFontColor(Color.BLACK);
    }

    private List<String> wrapText(String text, int maxCharsPerLine) {
        List<String> lines = new ArrayList<>();
        if (text == null) return lines;
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > maxCharsPerLine) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() > max ? text.substring(0, max - 3) + "..." : text;
    }

    private String sanitize(String text) {
        if (text == null) return "";
        // Replace non-ASCII and special characters for standard Type1 font compatibility
        return text.replaceAll("[^\\x20-\\x7E]", " ");
    }
}
