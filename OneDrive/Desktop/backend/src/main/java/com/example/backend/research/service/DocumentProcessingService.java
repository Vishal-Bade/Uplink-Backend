package com.example.backend.research.service;

import com.example.backend.common.util.TextChunker;
import com.example.backend.common.util.VectorMath;
import com.example.backend.llm.LlmProvider;
import com.example.backend.research.entity.DocumentChunk;
import com.example.backend.research.entity.DocumentEntity;
import com.example.backend.research.entity.ResearchRun;
import com.example.backend.research.entity.Source;
import com.example.backend.research.repository.DocumentChunkRepository;
import com.example.backend.research.repository.DocumentRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingService.class);
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentRepository documentRepository;
    private final LlmProvider llmProvider;
    private final TextChunker textChunker = new TextChunker(1000, 150);

    public DocumentProcessingService(
            DocumentChunkRepository documentChunkRepository,
            DocumentRepository documentRepository,
            LlmProvider llmProvider) {
        this.documentChunkRepository = documentChunkRepository;
        this.documentRepository = documentRepository;
        this.llmProvider = llmProvider;
    }

    @Transactional
    public List<DocumentChunk> processSourceIntoChunks(ResearchRun run, Source source) {
        // If chunks already exist for this run and source, return them
        List<DocumentChunk> existingChunks = documentChunkRepository.findBySourceId(source.getId());
        if (!existingChunks.isEmpty()) {
            return existingChunks;
        }

        String content = source.getCleanedContent();
        if (content == null || content.isBlank()) {
            content = source.getSnippet();
        }
        if (content == null || content.isBlank()) {
            return List.of();
        }

        List<TextChunker.Chunk> chunks = textChunker.chunk(content);
        List<DocumentChunk> persistedChunks = new ArrayList<>();

        for (TextChunker.Chunk c : chunks) {
            DocumentChunk chunk = new DocumentChunk(run, source, c.index(), c.content(), c.tokenCount());
            chunk.setStartCharOffset(c.startOffset());
            chunk.setEndCharOffset(c.endOffset());

            // Generate embedding for vector similarity
            try {
                float[] embedding = llmProvider.generateEmbedding(c.content());
                chunk.setEmbedding(VectorMath.serialize(embedding));
            } catch (Exception e) {
                log.warn("Embedding generation failed for chunk {}: {}", c.index(), e.getMessage());
                float[] fallback = VectorMath.generatePseudoEmbedding(c.content(), 1536);
                chunk.setEmbedding(VectorMath.serialize(fallback));
            }

            persistedChunks.add(documentChunkRepository.save(chunk));
        }

        log.info("Processed source '{}' into {} document chunks", source.getTitle(), persistedChunks.size());
        return persistedChunks;
    }

    @Transactional
    public DocumentEntity processUploadedPdf(DocumentEntity documentEntity, InputStream pdfInputStream) {
        try (PDDocument document = Loader.loadPDF(pdfInputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String extractedText = stripper.getText(document);

            documentEntity.setExtractedText(extractedText);
            documentEntity.setStatus("PROCESSED");
            log.info("Extracted {} characters from PDF: {}", extractedText.length(), documentEntity.getFileName());
        } catch (Exception e) {
            log.error("Failed to extract text from PDF {}: {}", documentEntity.getFileName(), e.getMessage());
            documentEntity.setStatus("FAILED");
        }

        return documentRepository.save(documentEntity);
    }
}
