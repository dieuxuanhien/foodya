package com.foodya.backend.application.service;

import com.foodya.backend.application.dto.AiChatHistoryView;
import com.foodya.backend.application.dto.AiChatResponseView;
import com.foodya.backend.application.dto.AiRecommendationItemView;
import com.foodya.backend.application.dto.CreateAiChatRequest;
import com.foodya.backend.application.port.out.AiChatHistoryPort;
import com.foodya.backend.application.port.out.AiDraftPort;
import com.foodya.backend.application.port.out.MenuItemPort;
import com.foodya.backend.application.port.out.RestaurantPort;
import com.foodya.backend.application.port.out.WeatherContextPort;
import com.foodya.backend.domain.model.RestaurantStatus;
import com.foodya.backend.domain.persistence.AiChatHistory;
import com.foodya.backend.domain.persistence.MenuItem;
import com.foodya.backend.domain.persistence.Restaurant;
import com.uber.h3core.H3Core;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class AiRecommendationService {

    private static final int H3_WEATHER_RES = 8;
    private static final int TOP_K_RECOMMENDATIONS = 5;
    private static final int WEATHER_CACHE_SECONDS = 600;
    private static final Pattern TOKEN_SPLITTER = Pattern.compile("[^a-zA-Z0-9]+");

    private final MenuItemPort menuItemPort;
    private final RestaurantPort restaurantPort;
    private final AiDraftPort aiDraftPort;
    private final WeatherContextPort weatherContextPort;
    private final AiChatHistoryPort aiChatHistoryPort;
    private final H3Core h3Core;
    private final Map<String, CachedWeather> weatherCache = new ConcurrentHashMap<>();

    public AiRecommendationService(MenuItemPort menuItemPort,
                                   RestaurantPort restaurantPort,
                                   AiDraftPort aiDraftPort,
                                   WeatherContextPort weatherContextPort,
                                   AiChatHistoryPort aiChatHistoryPort) {
        this.menuItemPort = menuItemPort;
        this.restaurantPort = restaurantPort;
        this.aiDraftPort = aiDraftPort;
        this.weatherContextPort = weatherContextPort;
        this.aiChatHistoryPort = aiChatHistoryPort;
        try {
            this.h3Core = H3Core.newInstance();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to initialize H3", ex);
        }
    }

    @Transactional
    public AiChatResponseView createChat(UUID customerUserId, CreateAiChatRequest request) {
        String normalizedPrompt = request.prompt().trim();
        List<String> tokens = tokenize(normalizedPrompt);
        Map<UUID, Restaurant> activeRestaurants = activeRestaurantsById();

        List<MenuItem> candidateItems = resolveCandidateItems(tokens, normalizedPrompt, activeRestaurants.keySet());
        List<AiRecommendationItemView> recommendations = recommend(candidateItems, activeRestaurants, tokens, normalizedPrompt);

        WeatherContext weather = resolveWeatherContext(request.lat(), request.lng());
        String aiDraft = generateAiDraft(normalizedPrompt, weather.rawWeather(), recommendations);
        String responseSummary = buildSummary(aiDraft, recommendations);

        AiChatHistory history = new AiChatHistory();
        history.setUserId(customerUserId);
        history.setPrompt(normalizedPrompt);
        history.setResponseSummary(responseSummary);
        history.setContextLatitude(request.lat());
        history.setContextLongitude(request.lng());
        history.setWeatherH3IndexRes8(weather.weatherH3IndexRes8());
        AiChatHistory saved = aiChatHistoryPort.save(history);

        return new AiChatResponseView(
                saved.getId(),
                saved.getPrompt(),
                saved.getResponseSummary(),
                recommendations,
                saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<AiChatHistoryView> history(UUID customerUserId) {
        return aiChatHistoryPort.findByUserIdOrderByCreatedAtDesc(customerUserId)
                .stream()
                .map(chat -> new AiChatHistoryView(chat.getId(), chat.getPrompt(), chat.getResponseSummary(), chat.getCreatedAt()))
                .toList();
    }

    private List<MenuItem> resolveCandidateItems(List<String> tokens,
                                                 String prompt,
                                                 Collection<UUID> activeRestaurantIds) {
        Map<UUID, MenuItem> dedup = new LinkedHashMap<>();

        for (String token : tokens) {
            for (MenuItem item : menuItemPort.findByActiveTrueAndDeletedAtIsNullAndNameContainingIgnoreCase(token)) {
                dedup.putIfAbsent(item.getId(), item);
            }
        }

        if (dedup.isEmpty()) {
            for (MenuItem item : menuItemPort.findByActiveTrueAndDeletedAtIsNullAndNameContainingIgnoreCase(prompt)) {
                dedup.putIfAbsent(item.getId(), item);
            }
        }

        if (dedup.isEmpty()) {
            for (MenuItem item : menuItemPort.findByActiveTrueAndDeletedAtIsNullAndNameContainingIgnoreCase("")) {
                dedup.putIfAbsent(item.getId(), item);
            }
        }

        return dedup.values().stream()
                .filter(MenuItem::isAvailable)
                .filter(item -> activeRestaurantIds.contains(item.getRestaurantId()))
                .toList();
    }

    private List<AiRecommendationItemView> recommend(List<MenuItem> candidateItems,
                                                     Map<UUID, Restaurant> activeRestaurants,
                                                     List<String> tokens,
                                                     String prompt) {
        String loweredPrompt = prompt.toLowerCase(Locale.ROOT);
        return candidateItems.stream()
                .sorted(Comparator
                        .comparingInt((MenuItem item) -> score(item, tokens, loweredPrompt)).reversed()
                        .thenComparing(MenuItem::getName)
                )
                .limit(TOP_K_RECOMMENDATIONS)
                .map(item -> {
                    Restaurant restaurant = activeRestaurants.get(item.getRestaurantId());
                    return new AiRecommendationItemView(
                            item.getId(),
                            item.getName(),
                            item.getRestaurantId(),
                            restaurant.getName(),
                            item.getPrice(),
                            reason(item, loweredPrompt)
                    );
                })
                .toList();
    }

    private int score(MenuItem item, List<String> tokens, String loweredPrompt) {
        String name = item.getName().toLowerCase(Locale.ROOT);
        int score = 0;
        for (String token : tokens) {
            if (name.contains(token)) {
                score += 2;
            }
        }
        if (name.contains(loweredPrompt)) {
            score += 4;
        }
        return score;
    }

    private String reason(MenuItem item, String loweredPrompt) {
        String loweredName = item.getName().toLowerCase(Locale.ROOT);
        if (loweredName.contains(loweredPrompt)) {
            return "best keyword match";
        }
        return "relevant popular option";
    }

    private Map<UUID, Restaurant> activeRestaurantsById() {
        return restaurantPort.findAll().stream()
                .filter(restaurant -> restaurant.getStatus() == RestaurantStatus.ACTIVE)
                .collect(LinkedHashMap::new, (map, restaurant) -> map.put(restaurant.getId(), restaurant), Map::putAll);
    }

    private WeatherContext resolveWeatherContext(BigDecimal lat, BigDecimal lng) {
        if (lat == null || lng == null) {
            return WeatherContext.empty();
        }

        String weatherH3Key = h3Core.geoToH3Address(lat.doubleValue(), lng.doubleValue(), H3_WEATHER_RES);
        Instant now = Instant.now();
        CachedWeather cached = weatherCache.get(weatherH3Key);
        if (cached != null && now.isBefore(cached.expiresAt())) {
            return new WeatherContext(cached.rawWeather(), weatherH3Key);
        }

        try {
            String raw = weatherContextPort.getCurrentWeatherRaw(lat.doubleValue(), lng.doubleValue());
            weatherCache.put(weatherH3Key, new CachedWeather(raw, now.plusSeconds(WEATHER_CACHE_SECONDS)));
            return new WeatherContext(raw, weatherH3Key);
        } catch (Exception ex) {
            return new WeatherContext(null, weatherH3Key);
        }
    }

    private String generateAiDraft(String prompt,
                                   String weatherRaw,
                                   List<AiRecommendationItemView> recommendations) {
        String candidateText = recommendations.stream()
                .map(item -> item.menuItemName() + " @ " + item.restaurantName())
                .reduce((a, b) -> a + ", " + b)
                .orElse("none");

        String weatherText = weatherRaw == null ? "not available" : truncate(weatherRaw, 200);
        String composedPrompt = "User request: " + prompt
                + "\nWeather context: " + weatherText
                + "\nCatalog candidates: " + candidateText
                + "\nReturn a concise recommendation explanation for these internal candidates only.";

        try {
            return aiDraftPort.generateRecommendationDraft(composedPrompt);
        } catch (Exception ex) {
            return null;
        }
    }

    private String buildSummary(String aiDraft, List<AiRecommendationItemView> recommendations) {
        if (aiDraft != null && !aiDraft.isBlank()) {
            return truncate(aiDraft.replaceAll("\\s+", " ").trim(), 400);
        }
        if (recommendations.isEmpty()) {
            return "No matching menu items were found from the active internal catalog.";
        }
        String joined = recommendations.stream()
                .map(AiRecommendationItemView::menuItemName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("items");
        return truncate("Recommended from active internal catalog: " + joined, 400);
    }

    private List<String> tokenize(String prompt) {
        String[] raw = TOKEN_SPLITTER.split(prompt.toLowerCase(Locale.ROOT));
        List<String> tokens = new ArrayList<>();
        for (String token : raw) {
            if (token.length() >= 3 && !tokens.contains(token)) {
                tokens.add(token);
            }
            if (tokens.size() >= 8) {
                break;
            }
        }
        return tokens;
    }

    private static String truncate(String raw, int maxLen) {
        if (raw.length() <= maxLen) {
            return raw;
        }
        return raw.substring(0, maxLen);
    }

    private record CachedWeather(String rawWeather, Instant expiresAt) {
    }

    private record WeatherContext(String rawWeather, String weatherH3IndexRes8) {

        private static WeatherContext empty() {
            return new WeatherContext(null, null);
        }
    }
}
