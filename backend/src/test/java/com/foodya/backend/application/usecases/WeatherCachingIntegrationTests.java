package com.foodya.backend.application.usecases;

import com.foodya.backend.application.dto.AiChatHistoryData;
import com.foodya.backend.application.dto.CreateAiChatRequest;
import com.foodya.backend.domain.entities.MenuItem;
import com.foodya.backend.domain.entities.Restaurant;
import com.foodya.backend.domain.entities.UserAccount;
import com.foodya.backend.domain.value_objects.RestaurantStatus;
import com.foodya.backend.domain.value_objects.UserRole;
import com.foodya.backend.application.dto.WeatherData;
import com.foodya.backend.domain.value_objects.UserStatus;
import com.foodya.backend.application.ports.out.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class WeatherCachingIntegrationTests {

    @Autowired
    private AiRecommendationService aiRecommendationService;

    @MockitoBean
    private AiEmbeddingPort aiEmbeddingPort;

    @MockitoBean
    private AiCatalogVectorPort aiCatalogVectorPort;

    @MockitoBean
    private MenuItemPort menuItemPort;

    @MockitoBean
    private RestaurantPort restaurantPort;

    @MockitoBean
    private UserAccountPort userAccountPort;

    @MockitoBean
    private AiDraftPort aiDraftPort;

    @MockitoBean
    private WeatherContextPort weatherContextPort;

    @MockitoBean
    private AiChatHistoryPort aiChatHistoryPort;

    @MockitoBean
    private SystemParameterPort systemParameterPort;

    @MockitoBean
    private GeoPort geoPort;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        var user = new UserAccount();
        user.setId(userId);
        user.setUsername("weather-customer");
        user.setEmail("weather@example.com");
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);

        var restaurant = new Restaurant();
        restaurant.setId(UUID.randomUUID());
        restaurant.setName("Pho Time");
        restaurant.setCuisineType("Vietnamese");
        restaurant.setStatus(RestaurantStatus.ACTIVE);
        restaurant.setOpen(true);
        restaurant.setLatitude(new BigDecimal("10.760000"));
        restaurant.setLongitude(new BigDecimal("106.660000"));
        restaurant.setAvgRating(new BigDecimal("4.5"));

        var menuItem = new MenuItem();
        menuItem.setId(UUID.randomUUID());
        menuItem.setRestaurantId(restaurant.getId());
        menuItem.setName("Pho Bo");
        menuItem.setDescription("Beef noodle soup");
        menuItem.setPrice(new BigDecimal("50000"));
        menuItem.setActive(true);
        menuItem.setAvailable(true);

        given(userAccountPort.findById(userId)).willReturn(Optional.of(user));
        given(restaurantPort.findAll()).willReturn(List.of(restaurant));

        given(menuItemPort.findByActiveTrueAndDeletedAtIsNullAndNameContainingIgnoreCase(anyString()))
                .willReturn(List.of(menuItem));
        given(menuItemPort.findByActiveTrueAndAvailableTrueAndDeletedAtIsNull())
                .willReturn(List.of(menuItem));

        given(aiCatalogVectorPort.isReady()).willReturn(false);
        given(aiCatalogVectorPort.countChunks()).willReturn(0L);
        given(aiCatalogVectorPort.searchByEmbedding(anyList(), anyInt())).willReturn(List.of());
        given(aiEmbeddingPort.embedText(anyString())).willReturn(List.of());

        given(geoPort.haversineKm(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .willReturn(new BigDecimal("1.200"));
        given(geoPort.geoToH3Index(anyDouble(), anyDouble(), anyInt()))
                .willReturn("8828308281fffff");

        given(weatherContextPort.getCurrentWeather(anyDouble(), anyDouble()))
                .willReturn(Optional.of(new WeatherData("Rain", "light rain", 28.1)));

        given(aiDraftPort.generateRecommendationDraft(anyString()))
                .willReturn("Try Pho Bo nearby");

        given(aiChatHistoryPort.findByUserIdOrderByCreatedAtDesc(userId)).willReturn(List.of());
        given(aiChatHistoryPort.save(any(AiChatHistoryData.class))).willAnswer(invocation -> {
            AiChatHistoryData data = invocation.getArgument(0);
            data.setId(UUID.randomUUID());
            data.setCreatedAt(OffsetDateTime.now());
            return data;
        });

        given(systemParameterPort.findById(anyString())).willReturn(Optional.empty());
    }

    @Test
    void createChat_reusesWeatherCacheForSameLocation() {
        CreateAiChatRequest request = new CreateAiChatRequest(
                "Suggest pho near me",
                new BigDecimal("10.776000"),
                new BigDecimal("106.700000")
        );

        var first = aiRecommendationService.createChat(userId, request);
        var second = aiRecommendationService.createChat(userId, request);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first.recommendations().size(), second.recommendations().size());

        verify(weatherContextPort, times(1))
                .getCurrentWeather(request.lat().doubleValue(), request.lng().doubleValue());
    }
}
