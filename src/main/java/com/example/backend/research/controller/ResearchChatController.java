package com.example.backend.research.controller;

import com.example.backend.common.exception.ResourceNotFoundException;
import com.example.backend.common.response.ApiResponse;
import com.example.backend.research.service.ResearchChatService;
import com.example.backend.security.ProjectSecurityService;
import com.example.backend.user.entity.User;
import com.example.backend.user.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/chat")
public class ResearchChatController {

    private final ResearchChatService researchChatService;
    private final ProjectSecurityService securityService;
    private final UserRepository userRepository;

    public static class ChatRequest {
        @NotBlank(message = "Question is required")
        private String question;
        private List<Map<String, String>> history;

        public ChatRequest() {}

        public ChatRequest(String question, List<Map<String, String>> history) {
            this.question = question;
            this.history = history;
        }

        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }

        public List<Map<String, String>> getHistory() { return history; }
        public void setHistory(List<Map<String, String>> history) { this.history = history; }
    }

    public ResearchChatController(
            ResearchChatService researchChatService,
            ProjectSecurityService securityService,
            UserRepository userRepository) {
        this.researchChatService = researchChatService;
        this.securityService = securityService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ResearchChatService.ChatResponse>> chat(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID projectId,
            @RequestBody ChatRequest request) {
        User user = getUser(userId);
        securityService.validateAndGetProject(projectId, user);

        ResearchChatService.ChatResponse response = researchChatService.chat(
                projectId,
                request.getQuestion(),
                request.getHistory()
        );
        return ResponseEntity.ok(ApiResponse.success("Chat answer generated", response));
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
