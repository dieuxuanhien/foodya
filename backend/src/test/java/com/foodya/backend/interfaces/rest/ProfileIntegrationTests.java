package com.foodya.backend.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import java.util.Objects;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void meAndUpdateAndChangePasswordFlow() throws Exception {
        String first = register("u1", "u1@example.com", "+84901000001");
        String access = objectMapper.readTree(first).path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("u1@example.com"));

        String updateReq = """
                {
                  "fullName":"User One Updated",
                  "email":"u1-updated@example.com",
                  "phoneNumber":"+84901000011",
                  "avatarUrl":"https://cdn.example.com/avatar/u1.png"
                }
                """;

        mockMvc.perform(patch("/api/v1/me")
                        .header("Authorization", "Bearer " + access)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(updateReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("u1-updated@example.com"));

        String pwdReq = """
                {
                  "currentPassword":"Strong@123",
                  "newPassword":"NewStrong@456",
                  "confirmPassword":"NewStrong@456"
                }
                """;

        mockMvc.perform(put("/api/v1/me/password")
                        .header("Authorization", "Bearer " + access)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(pwdReq))
                .andExpect(status().isOk());
    }

    @Test
    void updateRejectsDuplicateEmail() throws Exception {
                register("u2", "u2@example.com", "+84901000002");
        String second = register("u3", "u3@example.com", "+84901000003");

        String accessSecond = objectMapper.readTree(second).path("data").path("accessToken").asText();

        String updateReq = """
                {
                  "fullName":"User Three",
                  "email":"u2@example.com",
                  "phoneNumber":"+84901009999",
                  "avatarUrl":null
                }
                """;

        mockMvc.perform(patch("/api/v1/me")
                        .header("Authorization", "Bearer " + accessSecond)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(updateReq))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private String register(String username, String email, String phone) throws Exception {
        String body = """
                {
                  "username":"%s",
                  "email":"%s",
                  "phoneNumber":"%s",
                  "fullName":"%s",
                  "password":"Strong@123",
                  "role":"CUSTOMER"
                }
                """.formatted(username, email, phone, username);

        return mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                                .content(Objects.requireNonNull(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }
}
