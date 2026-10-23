package com.example.backend.user;

import com.example.backend.auth.dto.LoginRequest;
import com.example.backend.auth.dto.RegisterRequest;
import com.example.backend.user.dto.UpdateUserRequest;
import com.example.backend.user.enums.Role;
import com.example.backend.user.repository.UserRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.findByEmail("user@example.com").ifPresent(userRepository::delete);
        userRepository.findByUsername("myuser").ifPresent(userRepository::delete);
        userRepository.findByUsername("newusername").ifPresent(userRepository::delete);

        // Register and login to get JWT token
        RegisterRequest registerRequest = new RegisterRequest("user@example.com", "myuser", "password123", Role.STUDENT);
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        LoginRequest loginRequest = new LoginRequest("user@example.com", "password123");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        String responseStr = loginResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseStr);
        jwtToken = jsonNode.get("data").get("accessToken").asText();
    }

    @Test
    void testGetCurrentUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("user@example.com"));
    }

    @Test
    void testUpdateCurrentUser() throws Exception {
        UpdateUserRequest updateRequest = new UpdateUserRequest("newusername");

        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("newusername"));
    }

    @Test
    void testDeactivateUser() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/deactivate")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());

        // Login should now fail
        LoginRequest loginRequest = new LoginRequest("user@example.com", "password123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }
}
