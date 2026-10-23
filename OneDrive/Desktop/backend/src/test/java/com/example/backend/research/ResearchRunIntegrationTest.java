package com.example.backend.research;

import com.example.backend.auth.dto.LoginRequest;
import com.example.backend.auth.dto.RegisterRequest;
import com.example.backend.project.dto.CreateProjectRequest;
import com.example.backend.research.dto.CreateRunRequest;
import com.example.backend.research.entity.ResearchRun;
import com.example.backend.research.enums.ResearchDepth;
import com.example.backend.research.enums.RunStatus;
import com.example.backend.research.repository.ResearchRunRepository;
import com.example.backend.research.service.ResearchWorkflowCoordinator;
import com.example.backend.user.enums.Role;
import com.example.backend.workspace.dto.CreateWorkspaceRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ResearchRunIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResearchRunRepository researchRunRepository;

    @Autowired
    private ResearchWorkflowCoordinator workflowCoordinator;

    @Autowired
    private com.example.backend.workspace.repository.WorkspaceRepository workspaceRepository;

    private String userToken;
    private String otherUserToken;
    private UUID workspaceId;
    private UUID projectId;

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        if (workspaceId != null) {
            workspaceRepository.deleteById(workspaceId);
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        // Register primary user
        String email = "researcher_" + UUID.randomUUID() + "@example.com";
        String username = "researcher_" + UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest register = new RegisterRequest(email, username, "Password123!", Role.STUDENT);
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        userToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // Register secondary user for unauthorized access checks
        String otherEmail = "other_" + UUID.randomUUID() + "@example.com";
        String otherUsername = "other_" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(otherEmail, otherUsername, "Password123!", Role.STUDENT))))
                .andExpect(status().isCreated());

        MvcResult otherLoginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(otherEmail, "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        otherUserToken = objectMapper.readTree(otherLoginResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // Create Workspace
        CreateWorkspaceRequest wsRequest = new CreateWorkspaceRequest("Advanced Research Lab", "Autonomous Workspace");
        MvcResult wsResult = mockMvc.perform(post("/api/v1/workspaces")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wsRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        workspaceId = UUID.fromString(objectMapper.readTree(wsResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // Create Project
        CreateProjectRequest projectRequest = new CreateProjectRequest(
                workspaceId,
                "India Semiconductor Ecosystem",
                "Investigating manufacturing, incentives, and supply chains",
                "Analyze national policy, private investments, and technological bottlenecks"
        );
        MvcResult projResult = mockMvc.perform(post("/api/v1/projects")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        projectId = UUID.fromString(objectMapper.readTree(projResult.getResponse().getContentAsString())
                .path("data").path("id").asText());
    }

    @Test
    void testEndToEndAutonomousResearchPipeline() throws Exception {
        // 1. Submit Research Run (HTTP 202 Accepted)
        CreateRunRequest runRequest = new CreateRunRequest(
                "Analyze India's semiconductor manufacturing ecosystem, major investments, government policies, challenges, and future opportunities.",
                ResearchDepth.QUICK,
                true
        );

        MvcResult runResult = mockMvc.perform(post("/api/v1/projects/" + projectId + "/runs")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(runRequest)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        UUID runId = UUID.fromString(objectMapper.readTree(runResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // 2. Synchronously execute pipeline to guarantee deterministic completion for verification
        workflowCoordinator.executeResearchPipeline(runId);

        ResearchRun completedRun = researchRunRepository.findById(runId).orElseThrow();
        assertEquals(RunStatus.COMPLETED, completedRun.getStatus());
        assertTrue(completedRun.getTotalSourcesFound() > 0);
        assertTrue(completedRun.getTotalClaimsExtracted() > 0);

        // 3. Verify Run Details Endpoint
        mockMvc.perform(get("/api/v1/runs/" + runId)
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // 4. Verify Sources Endpoint
        mockMvc.perform(get("/api/v1/runs/" + runId + "/sources")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].url").exists())
                .andExpect(jsonPath("$.data[0].title").exists());

        // 5. Verify Claims & Evidence Endpoint
        mockMvc.perform(get("/api/v1/runs/" + runId + "/claims")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].claimText").exists())
                .andExpect(jsonPath("$.data[0].verificationStatus").exists())
                .andExpect(jsonPath("$.data[0].evidences").isArray());

        // 6. Verify Synthesized Report & Citations Endpoint
        mockMvc.perform(get("/api/v1/runs/" + runId + "/report")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").exists())
                .andExpect(jsonPath("$.data.executiveSummary").exists())
                .andExpect(jsonPath("$.data.keyFindings").exists())
                .andExpect(jsonPath("$.data.citations").isArray());

        // 7. Verify Export Artifacts (PDF & PPTX)
        MvcResult exportResult = mockMvc.perform(post("/api/v1/runs/" + runId + "/exports")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        JsonNode artifacts = objectMapper.readTree(exportResult.getResponse().getContentAsString()).path("data");
        assertTrue(artifacts.size() >= 2);
        UUID pdfArtifactId = UUID.fromString(artifacts.get(0).path("id").asText());

        // Download Artifact
        mockMvc.perform(get("/api/v1/artifacts/" + pdfArtifactId)
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"));

        // 8. Research Chat Endpoint
        String chatBody = "{\"question\":\"What are the primary investment figures?\"}";
        mockMvc.perform(post("/api/v1/projects/" + projectId + "/chat")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(chatBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answer").exists());

        // 9. Unauthorized Access Prevention Check
        // Other user attempting to access the research run must be forbidden
        mockMvc.perform(get("/api/v1/runs/" + runId)
                .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isForbidden());

        // Other user attempting to download artifact must be forbidden
        mockMvc.perform(get("/api/v1/artifacts/" + pdfArtifactId)
                .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isForbidden());
    }
}
