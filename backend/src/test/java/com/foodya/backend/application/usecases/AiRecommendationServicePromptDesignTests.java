package com.foodya.backend.application.usecases;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodya.backend.application.dto.AiChatHistoryData;
import com.foodya.backend.application.dto.CreateAiChatRequest;
import com.foodya.backend.application.dto.UserAccountData;
import com.foodya.backend.application.ports.out.AiCatalogVectorPort;
import com.foodya.backend.application.ports.out.AiChatHistoryPort;
import com.foodya.backend.application.ports.out.AiDraftPort;
import com.foodya.backend.application.ports.out.AiEmbeddingPort;
import com.foodya.backend.application.ports.out.GeoPort;
import com.foodya.backend.application.ports.out.MenuItemPort;
import com.foodya.backend.application.ports.out.RestaurantPort;
import com.foodya.backend.application.ports.out.SystemParameterPort;
import com.foodya.backend.application.ports.out.UserAccountPort;
import com.foodya.backend.application.ports.out.WeatherContextPort;
import com.foodya.backend.domain.entities.MenuItem;
import com.foodya.backend.domain.entities.Restaurant;
import com.foodya.backend.domain.value_objects.RestaurantStatus;
import com.foodya.backend.domain.value_objects.UserRole;
import com.foodya.backend.domain.value_objects.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiRecommendationServicePromptDesignTests {

    private AiEmbeddingPort aiEmbeddingPort;
    private AiCatalogVectorPort aiCatalogVectorPort;
    private MenuItemPort menuItemPort;
    private RestaurantPort restaurantPort;
    private UserAccountPort userAccountPort;
    private AiDraftPort aiDraftPort;
    private WeatherContextPort weatherContextPort;
    private AiChatHistoryPort aiChatHistoryPort;
    private SystemParameterPort systemParameterPort;
    private GeoPort geoPort;
    private AiRecommendationService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        aiEmbeddingPort = mock(AiEmbeddingPort.class);
        aiCatalogVectorPort = mock(AiCatalogVectorPort.class);
        menuItemPort = mock(MenuItemPort.class);
        restaurantPort = mock(RestaurantPort.class);
        userAccountPort = mock(UserAccountPort.class);
        aiDraftPort = mock(AiDraftPort.class);
        weatherContextPort = mock(WeatherContextPort.class);
        aiChatHistoryPort = mock(AiChatHistoryPort.class);
        systemParameterPort = mock(SystemParameterPort.class);
        geoPort = mock(GeoPort.class);

        service = new AiRecommendationService(
                aiEmbeddingPort,
                aiCatalogVectorPort,
                menuItemPort,
                restaurantPort,
                userAccountPort,
                aiDraftPort,
                weatherContextPort,
                aiChatHistoryPort,
                systemParameterPort,
                geoPort,
                new ObjectMapper()
        );

        userId = UUID.randomUUID();
        UserAccountData user = new UserAccountData();
        user.setId(userId);
        user.setUsername("prompt-customer");
        user.setEmail("prompt@example.com");
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);

        Restaurant restaurant = new Restaurant();
        restaurant.setId(UUID.randomUUID());
        restaurant.setName("Pho Time");
        restaurant.setCuisineType("Vietnamese");
        restaurant.setStatus(RestaurantStatus.ACTIVE);
        restaurant.setOpen(true);
        restaurant.setLatitude(new BigDecimal("10.760000"));
        restaurant.setLongitude(new BigDecimal("106.660000"));
        restaurant.setAvgRating(new BigDecimal("4.5"));

        MenuItem menuItem = new MenuItem();
        menuItem.setId(UUID.randomUUID());
        menuItem.setRestaurantId(restaurant.getId());
        menuItem.setName("Pho Bo");
        menuItem.setDescription("Beef noodle soup");
        menuItem.setPrice(new BigDecimal("50000"));
        menuItem.setActive(true);
        menuItem.setAvailable(true);

        when(userAccountPort.findById(userId)).thenReturn(Optional.of(user));
        when(restaurantPort.findAll()).thenReturn(List.of(restaurant));
        when(menuItemPort.findByActiveTrueAndDeletedAtIsNullAndNameContainingIgnoreCase(anyString()))
                .thenReturn(List.of(menuItem));
        when(menuItemPort.findByActiveTrueAndAvailableTrueAndDeletedAtIsNull())
                .thenReturn(List.of(menuItem));
        when(aiCatalogVectorPort.isReady()).thenReturn(false);
        when(aiCatalogVectorPort.countChunks()).thenReturn(0L);
        when(aiCatalogVectorPort.searchByEmbedding(anyList(), anyInt())).thenReturn(List.of());
        when(aiEmbeddingPort.embedText(anyString())).thenReturn(List.of());
        when(geoPort.haversineKm(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(new BigDecimal("1.200"));
        when(weatherContextPort.getCurrentWeatherRaw(anyDouble(), anyDouble()))
                .thenReturn("{\"weather\":[{\"main\":\"Rain\",\"description\":\"light rain\"}],\"main\":{\"temp\":28.1}}");
        when(aiDraftPort.generateRecommendationDraft(anyString()))
                .thenReturn("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"I found cozy Foodya picks for you.\"}]}}]}");
        when(aiChatHistoryPort.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(aiChatHistoryPort.save(any(AiChatHistoryData.class))).thenAnswer(invocation -> {
            AiChatHistoryData data = invocation.getArgument(0);
            data.setId(UUID.randomUUID());
            data.setCreatedAt(OffsetDateTime.now());
            return data;
        });
        when(systemParameterPort.findById(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void createChatSendsGuardedFriendlyPromptToAiDraftProvider() {
        service.createChat(
                userId,
                new CreateAiChatRequest(
                        "Suggest warm food for rainy weather",
                        new BigDecimal("10.776000"),
                        new BigDecimal("106.700000")
                )
        );

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiDraftPort).generateRecommendationDraft(promptCaptor.capture());
        String prompt = promptCaptor.getValue();

        assertTrue(prompt.contains("Foodya's food recommendation assistant"));
        assertTrue(prompt.contains("Match the customer's language when clear"));
        assertTrue(prompt.contains("Do not invent or mention menu item names"));
        assertTrue(prompt.contains("Return plain text only"));
        assertTrue(prompt.contains("One sentence, maximum 180 characters"));
        assertTrue(prompt.contains("User request: Suggest warm food for rainy weather"));
        assertTrue(prompt.contains("Recent chat context: none"));
        assertTrue(prompt.contains("Weather context: Rain"));
        assertTrue(prompt.contains("Intent hints:"));
        assertTrue(prompt.contains("Foodya catalog candidates:"));
        assertTrue(prompt.contains("menuItem=\"Pho Bo\""));
    }

    @Test
    void createChatDoesNotExposeHallucinatedModelEntityInSummary() {
        when(aiDraftPort.generateRecommendationDraft(anyString()))
                .thenReturn("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Try Dragon Sushi Palace for 30k delivery.\"}]}}]}");

        var response = service.createChat(userId, new CreateAiChatRequest("Suggest dinner", null, null));

        assertEquals("I found Foodya options that match your request. Pick a card below to view details.",
                response.responseSummary());
        assertFalse(response.responseSummary().contains("Dragon"));
        assertFalse(response.responseSummary().contains("Sushi"));
        assertFalse(response.responseSummary().contains("30k"));
    }

    @Test
    void createChatFallsBackWhenModelIntroIsOverlong() {
        when(aiDraftPort.generateRecommendationDraft(anyString())).thenReturn("A".repeat(181));

        var response = service.createChat(userId, new CreateAiChatRequest("Suggest dinner", null, null));

        assertEquals("I found Foodya options that match your request. Pick a card below to view details.",
                response.responseSummary());
    }

    @Test
    void createChatSkipsAiDraftWhenNoRecommendationsExist() {
        when(menuItemPort.findByActiveTrueAndDeletedAtIsNullAndNameContainingIgnoreCase(anyString()))
                .thenReturn(List.of());
        when(menuItemPort.findByActiveTrueAndAvailableTrueAndDeletedAtIsNull())
                .thenReturn(List.of());

        var response = service.createChat(userId, new CreateAiChatRequest("Find unicorn noodles", null, null));

        assertTrue(response.recommendations().isEmpty());
        assertEquals("I couldn't find a matching Foodya option right now. Try a broader keyword, lower rating or budget filters, or another location.",
                response.responseSummary());
        verify(aiDraftPort, never()).generateRecommendationDraft(anyString());
    }

    @Test
    void clearHistoryDeletesOnlyCurrentUserHistory() {
        when(aiChatHistoryPort.deleteByUserId(userId)).thenReturn(2L);

        service.clearHistory(userId);

        verify(aiChatHistoryPort).deleteByUserId(userId);
    }
}
