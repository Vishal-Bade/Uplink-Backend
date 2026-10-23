package com.example.backend.research.service;

import com.example.backend.llm.LlmProvider;
import com.example.backend.llm.LlmResponse;
import com.example.backend.project.entity.Project;
import com.example.backend.project.repository.ProjectRepository;
import com.example.backend.research.entity.DocumentChunk;
import com.example.backend.research.repository.DocumentChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ResearchChatService {

    private static final Logger log = LoggerFactory.getLogger(ResearchChatService.class);
    private final ProjectRepository projectRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final LlmProvider llmProvider;

    public record ChatResponse(
            String answer,
            List<String> sourcesUsed,
            long tokensUsed
    ) {}

    public ResearchChatService(
            ProjectRepository projectRepository,
            DocumentChunkRepository documentChunkRepository,
            LlmProvider llmProvider) {
        this.projectRepository = projectRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.llmProvider = llmProvider;
    }

    public ChatResponse chat(UUID projectId, String question, List<Map<String, String>> history) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        log.info("Processing research chat query for project '{}': {}", project.getName(), question);

        // Retrieve stored evidence strictly scoped to this project
        List<DocumentChunk> projectChunks = documentChunkRepository.findByProjectIdWithSource(projectId);
        List<String> sourcesUsed = new ArrayList<>();
        StringBuilder evidenceContext = new StringBuilder();

        for (int i = 0; i < Math.min(projectChunks.size(), 6); i++) {
            DocumentChunk chunk = projectChunks.get(i);
            String title = chunk.getSource() != null ? chunk.getSource().getTitle() : "Document";
            String url = chunk.getSource() != null ? chunk.getSource().getUrl() : "";
            evidenceContext.append("[").append(i + 1).append("] ").append(title)
                    .append(" (").append(url).append("):\n")
                    .append(chunk.getContent()).append("\n\n");
            if (!url.isBlank() && !sourcesUsed.contains(url)) {
                sourcesUsed.add(url);
            }
        }

        String systemPrompt = "You are an autonomous research assistant. Answer the user's inquiry based strictly on the provided research project evidence. " +
                "Cite sources using [Source Title](URL). If the evidence is insufficient to answer the question with certainty, explicitly explain that additional research is required.";

        StringBuilder conversationPrompt = new StringBuilder();
        if (history != null && !history.isEmpty()) {
            conversationPrompt.append("Previous Conversation:\n");
            for (Map<String, String> msg : history) {
                conversationPrompt.append(msg.getOrDefault("role", "user")).append(": ")
                        .append(msg.getOrDefault("content", "")).append("\n");
            }
            conversationPrompt.append("\n");
        }

        conversationPrompt.append("Project: ").append(project.getName()).append("\n")
                .append("Available Project Evidence:\n")
                .append(evidenceContext.length() > 0 ? evidenceContext.toString() : "No evidence documents have been gathered for this project yet.\n\n")
                .append("User Question: ").append(question);

        LlmResponse<String> response = llmProvider.generateText(conversationPrompt.toString(), systemPrompt);

        return new ChatResponse(response.data(), sourcesUsed, response.totalTokens());
    }
}
