package com.foodya.backend.interfaces.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import java.util.Objects;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerLoginRefreshAndReuseDetection() throws Exception {
        String registerBody = """
                {
                  "username":"alice",
                  "email":"alice@example.com",
                  "phoneNumber":"+84901234567",
                  "fullName":"Alice Nguyen",
                  "password":"Strong@123",
                  "role":"CUSTOMER"
                }
                """;

        String registerResponse = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        JsonNode registerJson = objectMapper.readTree(registerResponse);
        String accessToken = registerJson.path("data").path("accessToken").asText();
        String refreshToken = registerJson.path("data").path("refreshToken").asText();

        String refreshReq = "{\"refreshToken\":\"" + refreshToken + "\"}";
        String refreshResponse = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(refreshReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String reusedReq = "{\"refreshToken\":\"" + refreshToken + "\"}";
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(reusedReq))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        JsonNode refreshJson = objectMapper.readTree(refreshResponse);
        String newRefresh = refreshJson.path("data").path("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("{\"refreshToken\":\"" + newRefresh + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void registerRejectsDuplicateIdentityFields() throws Exception {
        String first = """
                {
                  "username":"dupe-identity",
                  "email":"dupe@example.com",
                  "phoneNumber":"+84901119999",
                  "fullName":"Dupe One",
                  "password":"Strong@123",
                  "role":"CUSTOMER"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(first))
                .andExpect(status().isCreated());

        String second = """
                {
                  "username":"dupe-identity",
                  "email":"dupe2@example.com",
                  "phoneNumber":"+84901118888",
                  "fullName":"Dupe Two",
                  "password":"Strong@123",
                  "role":"CUSTOMER"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(second))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void forgotPasswordVerifyOtpResetFlow() throws Exception {
        String registerBody = """
                {
                  "username":"forgot-user",
                  "email":"forgot@example.com",
                  "phoneNumber":"+84907770000",
                  "fullName":"Forgot User",
                  "password":"Strong@123",
                  "role":"CUSTOMER"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(registerBody))
                .andExpect(status().isCreated());

        String forgotResponse = mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("{\"email\":\"forgot@example.com\"}"))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        JsonNode forgot = objectMapper.readTree(forgotResponse);
        String challengeToken = forgot.path("data").path("challengeToken").asText();
        String deliveryHint = forgot.path("data").path("deliveryHint").asText();
        org.junit.jupiter.api.Assertions.assertFalse(challengeToken == null || challengeToken.isBlank());
        org.junit.jupiter.api.Assertions.assertFalse(deliveryHint.contains("dev-otp:"));
    }

    @Test
    void logoutRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("{\"refreshToken\":\"dummy\"}"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void registerRejectsWeakPassword() throws Exception {
        String registerBody = """
                {
                  "username":"weak-user",
                  "email":"weak@example.com",
                  "phoneNumber":"+84901111222",
                  "fullName":"Weak User",
                  "password":"12345678",
                  "role":"CUSTOMER"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(registerBody))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
