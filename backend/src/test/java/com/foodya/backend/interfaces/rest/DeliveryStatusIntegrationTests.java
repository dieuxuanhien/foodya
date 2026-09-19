package com.foodya.backend.interfaces.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodya.backend.application.ports.out.TokenPort;
import com.foodya.backend.domain.value_objects.DriverSessionStatus;
import com.foodya.backend.domain.value_objects.UserRole;
import com.foodya.backend.domain.value_objects.UserStatus;
import com.foodya.backend.domain.entities.UserAccount;
import com.foodya.backend.infrastructure.mapper.UserAccountMapper;
import com.foodya.backend.infrastructure.persistence.models.UserAccountPersistenceModel;
import com.foodya.backend.infrastructure.repository.DriverOnlineSessionRepository;
import com.foodya.backend.infrastructure.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Objects;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the Grab-style driver lifecycle toggle endpoints.
 * All tests operate against a real H2 in-memory database (test profile).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeliveryStatusIntegrationTests {

    private static final String BASE = "/api/v1/delivery/status";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private DriverOnlineSessionRepository driverOnlineSessionRepository;

    @Autowired
    private TokenPort tokenService;

    @Autowired
    private UserAccountMapper userAccountMapper;

    @BeforeEach
    void setUp() {
        driverOnlineSessionRepository.deleteAll();
    }

    // -------------------------------------------------------------------------
    // Happy path: online → status → update location → offline
    // -------------------------------------------------------------------------

    @Test
    void toggleOnlineReturnsOnlineSession() throws Exception {
        UserAccount driver = seedDriver("driver-on-1");
        String token = tokenService.issueAccessToken(driver, UUID.randomUUID().toString());

        mockMvc.perform(post(BASE + "/online")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(DriverSessionStatus.ONLINE.name()))
                .andExpect(jsonPath("$.data.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.data.activeOrderCount").value(0));
    }

    @Test
    void getStatusReturnsOnlineAfterToggle() throws Exception {
        UserAccount driver = seedDriver("driver-status-1");
        String token = tokenService.issueAccessToken(driver, UUID.randomUUID().toString());

        // Go online first
        mockMvc.perform(post(BASE + "/online")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Now check status
        mockMvc.perform(get(BASE)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(DriverSessionStatus.ONLINE.name()));
    }

    @Test
    void getStatusReturnsOfflineWhenNoSession() throws Exception {
        UserAccount driver = seedDriver("driver-status-off");
        String token = tokenService.issueAccessToken(driver, UUID.randomUUID().toString());

        mockMvc.perform(get(BASE)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(DriverSessionStatus.OFFLINE.name()))
                .andExpect(jsonPath("$.data.sessionId").doesNotExist());
    }

    @Test
    void updateLocationReturnsUpdatedCoordinates() throws Exception {
        UserAccount driver = seedDriver("driver-loc-1");
        String token = tokenService.issueAccessToken(driver, UUID.randomUUID().toString());

        // Must be online first
        mockMvc.perform(post(BASE + "/online")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        String locationBody = """
                {"lat": 10.7769, "lng": 106.7009}
                """;

        mockMvc.perform(put(BASE + "/location")
                        .header("Authorization", "Bearer " + token)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(locationBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentLat").value(10.7769))
                .andExpect(jsonPath("$.data.currentLng").value(106.7009))
                .andExpect(jsonPath("$.data.status").value(DriverSessionStatus.ONLINE.name()));
    }

    @Test
    void toggleOfflineWhileOnlineReturnsOffline() throws Exception {
        UserAccount driver = seedDriver("driver-off-1");
        String token = tokenService.issueAccessToken(driver, UUID.randomUUID().toString());

        // Go online
        mockMvc.perform(post(BASE + "/online")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Go offline
        mockMvc.perform(post(BASE + "/offline")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(DriverSessionStatus.OFFLINE.name()))
                .andExpect(jsonPath("$.data.endedAt").isNotEmpty());
    }

    // -------------------------------------------------------------------------
    // Error paths
    // -------------------------------------------------------------------------

    @Test
    void toggleOnlineWhenAlreadyOnlineReturns409() throws Exception {
        UserAccount driver = seedDriver("driver-dbl-online");
        String token = tokenService.issueAccessToken(driver, UUID.randomUUID().toString());

        // First toggle — should succeed
        mockMvc.perform(post(BASE + "/online")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Second toggle — should fail with 409 / VALIDATION_FAILED
        mockMvc.perform(post(BASE + "/online")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void toggleOfflineWhenAlreadyOfflineReturns422() throws Exception {
        UserAccount driver = seedDriver("driver-dbl-offline");
        String token = tokenService.issueAccessToken(driver, UUID.randomUUID().toString());

        // No active session at all — should fail
        mockMvc.perform(post(BASE + "/offline")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void updateLocationWhileOfflineReturns422() throws Exception {
        UserAccount driver = seedDriver("driver-loc-offline");
        String token = tokenService.issueAccessToken(driver, UUID.randomUUID().toString());

        // No session yet — location update should fail
        String locationBody = """
                {"lat": 10.7769, "lng": 106.7009}
                """;

        mockMvc.perform(put(BASE + "/location")
                        .header("Authorization", "Bearer " + token)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(locationBody))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get(BASE))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(BASE + "/online"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotAccessDriverEndpoints() throws Exception {
        // Register as CUSTOMER (not DELIVERY)
        String registerBody = """
                {
                  "username":"customer-tries-driver",
                  "email":"customer-driver@test.local",
                  "phoneNumber":"+84901230001",
                  "fullName":"Test Customer",
                  "password":"Strong@123",
                  "role":"CUSTOMER"
                }
                """;

        String registerResponse = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(registerResponse);
        String token = json.path("data").path("accessToken").asText();

        mockMvc.perform(post(BASE + "/online")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidLocationCoordinatesReturn422() throws Exception {
        UserAccount driver = seedDriver("driver-bad-loc");
        String token = tokenService.issueAccessToken(driver, UUID.randomUUID().toString());

        mockMvc.perform(post(BASE + "/online")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Latitude out of range
        String badBody = """
                {"lat": 999.0, "lng": 106.7009}
                """;

        mockMvc.perform(put(BASE + "/location")
                        .header("Authorization", "Bearer " + token)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(badBody))
                .andExpect(status().isUnprocessableEntity());
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private UserAccount seedDriver(String stem) {
        UserAccountPersistenceModel model = new UserAccountPersistenceModel();
        model.setUsername(stem);
        model.setEmail(stem + "@test.local");
        model.setPhoneNumber("+8490" + String.format("%07d", Math.abs(stem.hashCode() % 10000000)));
        model.setFullName(stem);
        model.setRole(UserRole.DELIVERY);
        model.setStatus(UserStatus.ACTIVE);
        model.setPasswordHash("$2a$10$abcdefghijklmnopqrstuv");
        return userAccountMapper.toDomain(userAccountRepository.save(model));
    }
}
