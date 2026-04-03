package com.foodya.backend.application.usecases;

import com.foodya.backend.application.dto.AiChatHistoryView;
import com.foodya.backend.application.dto.AiChatResponseView;
import com.foodya.backend.application.dto.AiCatalogChunkDocument;
import com.foodya.backend.application.dto.AiCatalogVectorHit;
import com.foodya.backend.application.dto.AiRecommendationItemView;
import com.foodya.backend.application.dto.CreateAiChatRequest;
import com.foodya.backend.application.dto.AiChatHistoryModel;
import com.foodya.backend.application.ports.in.AiRecommendationUseCase;
import com.foodya.backend.application.ports.out.AiCatalogVectorPort;
import com.foodya.backend.application.ports.out.AiChatHistoryPort;
import com.foodya.backend.application.ports.out.AiDraftPort;
import com.foodya.backend.application.ports.out.AiEmbeddingPort;
import com.foodya.backend.application.ports.out.GeoPort;
import com.foodya.backend.application.ports.out.MenuItemPort;
import com.foodya.backend.application.ports.out.RestaurantPort;
import com.foodya.backend.application.ports.out.SystemParameterPort;
import com.foodya.backend.application.ports.out.WeatherContextPort;
import com.foodya.backend.domain.entities.SystemParameter;
import com.foodya.backend.domain.value_objects.RestaurantStatus;
import com.foodya.backend.domain.entities.MenuItem;
import com.foodya.backend.domain.entities.Restaurant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uber.h3core.H3Core;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiRecommendationService implements AiRecommendationUseCase {

    private static final int H3_WEATHER_RES = 8;
    private static final int TOP_K_RECOMMENDATIONS = 5;
    private static final int WEATHER_CACHE_SECONDS = 600;
    private static final int RAG_TOP_K = 40;
    private static final int RAG_REFRESH_SECONDS = 900;
    private static final int DEFAULT_MAX_SHIPPING_KM = 15;
    private static final Pattern TOKEN_SPLITTER = Pattern.compile("[^\\p{L}\\p{Nd}]+");

    private static final Set<String> NEARBY_HINTS = Set.of("near", "nearby", "gan", "quanhday", "nhahanggan", "restaurantnear");
    private static final Set<String> SPICY_HINTS = Set.of("cay", "spicy", "hot", "chili", "ot", "sate");
    private static final Set<String> NOT_SPICY_HINTS = Set.of("khongcay", "itcay", "notspicy", "mild");
    private static final Set<String> WARM_HINTS = Set.of("nong", "am", "warm", "hot", "soup", "nuoc", "broth");
    private static final Set<String> COLD_HINTS = Set.of("cold", "iced", "lanh", "mat", "salad", "ice");
    private static final Set<String> VEGETARIAN_HINTS = Set.of("chay", "vegetarian", "vegan", "plantbased", "dauhu", "tofu");
    private static final Set<String> NON_VEGETARIAN_HINTS = Set.of("man", "thit", "haisan", "meat", "seafood", "beef", "chicken", "pork");
    private static final Set<String> PROTEIN_HINTS = Set.of("bo", "beef", "ga", "chicken", "heo", "pork", "hai", "seafood", "tom", "shrimp", "ca", "fish", "tofu");
    private static final Set<String> SNACK_HINTS = Set.of("anvat", "snack", "starter", "light");
    private static final Set<String> MAIN_MEAL_HINTS = Set.of("anchinh", "main", "meal", "com", "bun", "pho", "noodle", "rice");
    private static final Set<String> FAST_FOOD_HINTS = Set.of("fastfood", "fast", "burger", "pizza", "fries", "fried", "garan");
    private static final Set<String> BUDGET_HINTS = Set.of("gia re", "re", "budget", "tiet kiem", "cheap");
    private static final Set<String> TOTAL_BUDGET_HINTS = Set.of("tong", "ca nhom", "all in", "allin", "overall");
    private static final Pattern PRICE_RANGE_PATTERN = Pattern.compile("(?:tu|from)\\s*(\\d[\\d\\.,]*)\\s*(k|nghin|tr|m|vnd)?\\s*(?:den|toi|to|-)\\s*(\\d[\\d\\.,]*)\\s*(k|nghin|tr|m|vnd)?");
    private static final Pattern PRICE_RANGE_GENERIC_PATTERN = Pattern.compile("(\\d[\\d\\.,]*)\\s*(k|nghin|tr|m|vnd)?\\s*(?:-|den|toi|to)\\s*(\\d[\\d\\.,]*)\\s*(k|nghin|tr|m|vnd)?");
    private static final Pattern PRICE_MAX_PATTERN = Pattern.compile("(?:duoi|toi da|max|khong qua|under|<=)\\s*(\\d[\\d\\.,]*)\\s*(k|nghin|tr|m|vnd)?");
    private static final Pattern PRICE_MIN_PATTERN = Pattern.compile("(?:tren|toi thieu|min|over|>=)\\s*(\\d[\\d\\.,]*)\\s*(k|nghin|tr|m|vnd)?");
    private static final Pattern PRICE_APPROX_PATTERN = Pattern.compile("(?:tam|khoang|around|about)\\s*(\\d[\\d\\.,]*)\\s*(k|nghin|tr|m|vnd)?");
    private static final Pattern RATING_MIN_PATTERN = Pattern.compile("(?:rating|danh gia|danhgia|sao)\\s*(?:tu|>=|tren|toi thieu|min|tro len)?\\s*(\\d(?:[\\.,]\\d)?)");
    private static final Pattern RATING_MIN_SUFFIX_PATTERN = Pattern.compile("(\\d(?:[\\.,]\\d)?)\\s*(?:sao|star)\\s*(?:tro len|or more|\\+)");
    private static final Pattern PEOPLE_COUNT_PATTERN = Pattern.compile("(?:cho|for)\\s*(\\d{1,2})\\s*(?:nguoi|people|person)");
    private static final BigDecimal CHEAP_PRICE_MAX = BigDecimal.valueOf(60000);

    private static final String EMPTY_JSON = "{}";

    private final AiEmbeddingPort aiEmbeddingPort;
    private final AiCatalogVectorPort aiCatalogVectorPort;
    private final MenuItemPort menuItemPort;
    private final RestaurantPort restaurantPort;
    private final AiDraftPort aiDraftPort;
    private final WeatherContextPort weatherContextPort;
    private final AiChatHistoryPort aiChatHistoryPort;
    private final SystemParameterPort systemParameterPort;
    private final GeoPort geoPort;
    private final H3Core h3Core;
    private final ObjectMapper objectMapper;
    private final Map<String, CachedWeather> weatherCache = new ConcurrentHashMap<>();
    private volatile Instant lastRagSnapshotAt;

    public AiRecommendationService(AiEmbeddingPort aiEmbeddingPort,
                                   AiCatalogVectorPort aiCatalogVectorPort,
                                   MenuItemPort menuItemPort,
                                   RestaurantPort restaurantPort,
                                   AiDraftPort aiDraftPort,
                                   WeatherContextPort weatherContextPort,
                                   AiChatHistoryPort aiChatHistoryPort,
                                   SystemParameterPort systemParameterPort,
                                   GeoPort geoPort,
                                   ObjectMapper objectMapper) {
                    this.aiEmbeddingPort = aiEmbeddingPort;
                    this.aiCatalogVectorPort = aiCatalogVectorPort;
        this.menuItemPort = menuItemPort;
        this.restaurantPort = restaurantPort;
        this.aiDraftPort = aiDraftPort;
        this.weatherContextPort = weatherContextPort;
        this.aiChatHistoryPort = aiChatHistoryPort;
        this.systemParameterPort = systemParameterPort;
        this.geoPort = geoPort;
        this.objectMapper = objectMapper;
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
        RecommendationIntent intent = parseIntent(normalizedPrompt, tokens);
        Map<UUID, Restaurant> activeRestaurants = activeRestaurantsById();
        Map<UUID, MenuItem> activeMenuItemsById = activeMenuItemsById(activeRestaurants.keySet());
        WeatherContext weather = resolveWeatherContext(request.lat(), request.lng());
        BigDecimal maxShippingDistanceKm = maxShippingDistanceKm();
        boolean locationProvided = request.lat() != null && request.lng() != null;
        String conversationContext = recentConversationContext(customerUserId);
        Map<UUID, Double> vectorSimilarityByMenuItem = retrieveVectorSimilarity(
            normalizedPrompt,
            conversationContext,
            activeRestaurants,
            activeMenuItemsById
        );

        Map<UUID, BigDecimal> distanceByRestaurant = computeDistanceByRestaurant(
            activeRestaurants,
            request.lat(),
            request.lng(),
            maxShippingDistanceKm
        );

        List<MenuItem> candidateItems = resolveCandidateItems(
            tokens,
            normalizedPrompt,
            activeRestaurants,
            activeMenuItemsById,
            vectorSimilarityByMenuItem,
            distanceByRestaurant,
            locationProvided,
            intent
        );
        List<AiRecommendationItemView> recommendations = recommend(
            candidateItems,
            activeRestaurants,
            distanceByRestaurant,
            vectorSimilarityByMenuItem,
            tokens,
            normalizedPrompt,
            intent,
            weather.signal()
        );

        String aiDraft = generateAiDraft(normalizedPrompt,
            weather,
            recommendations,
            intent,
            maxShippingDistanceKm,
            conversationContext);
        String responseSummary = buildSummary(aiDraft, recommendations, intent);

        AiChatHistoryModel history = new AiChatHistoryModel();
        history.setUserId(customerUserId);
        history.setPrompt(normalizedPrompt);
        history.setResponseSummary(responseSummary);
        history.setContextLatitude(request.lat());
        history.setContextLongitude(request.lng());
        history.setWeatherH3IndexRes8(weather.weatherH3IndexRes8());
        AiChatHistoryModel saved = aiChatHistoryPort.save(history);

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
                                                 Map<UUID, Restaurant> activeRestaurants,
                                                 Map<UUID, MenuItem> activeMenuItemsById,
                                                 Map<UUID, Double> vectorSimilarityByMenuItem,
                                                 Map<UUID, BigDecimal> distanceByRestaurant,
                                                 boolean locationProvided,
                                                 RecommendationIntent intent) {
        Map<UUID, MenuItem> dedup = new LinkedHashMap<>();
        Collection<UUID> activeRestaurantIds = activeRestaurants.keySet();

        vectorSimilarityByMenuItem.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .forEach(entry -> {
                    MenuItem item = activeMenuItemsById.get(entry.getKey());
                    if (item != null) {
                        dedup.putIfAbsent(item.getId(), item);
                    }
                });

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
            for (MenuItem item : menuItemPort.findByActiveTrueAndAvailableTrueAndDeletedAtIsNull()) {
                dedup.putIfAbsent(item.getId(), item);
            }
        }

        List<MenuItem> scoped = dedup.values().stream()
                .filter(MenuItem::isAvailable)
                .filter(item -> activeRestaurantIds.contains(item.getRestaurantId()))
                .toList();

        List<MenuItem> strictMatched = scoped.stream()
                .filter(item -> !intent.requireNotSpicy() || !isLikelySpicy(item))
                .filter(item -> !intent.requireVegetarian() || isLikelyVegetarian(item))
                .filter(item -> intent.requiredProteins().isEmpty() || hasAnyToken(item, intent.requiredProteins()))
                .filter(item -> intent.requireWarm() == null || (intent.requireWarm() ? !isLikelyCold(item) : !isLikelyWarm(item)))
                .filter(item -> priceWithinIntent(item.getPrice(), intent))
                .filter(item -> restaurantMatchesRating(activeRestaurants.get(item.getRestaurantId()), intent))
                .filter(item -> !locationProvided || distanceByRestaurant.containsKey(item.getRestaurantId()))
                .toList();

        if (!strictMatched.isEmpty()) {
            return strictMatched;
        }

        if (intent.hasHardConstraints()) {
            List<MenuItem> broadened = menuItemPort.findByActiveTrueAndAvailableTrueAndDeletedAtIsNull().stream()
                    .filter(item -> activeRestaurantIds.contains(item.getRestaurantId()))
                    .filter(item -> !locationProvided || distanceByRestaurant.containsKey(item.getRestaurantId()))
                    .filter(item -> !intent.requireNotSpicy() || !isLikelySpicy(item))
                    .filter(item -> !intent.requireVegetarian() || isLikelyVegetarian(item))
                    .filter(item -> intent.requiredProteins().isEmpty() || hasAnyToken(item, intent.requiredProteins()))
                    .filter(item -> intent.requireWarm() == null || (intent.requireWarm() ? !isLikelyCold(item) : !isLikelyWarm(item)))
                    .filter(item -> priceWithinIntent(item.getPrice(), intent))
                    .filter(item -> restaurantMatchesRating(activeRestaurants.get(item.getRestaurantId()), intent))
                    .toList();
            if (!broadened.isEmpty()) {
                return broadened;
            }
        }

        return scoped.stream()
            .filter(item -> !locationProvided || distanceByRestaurant.containsKey(item.getRestaurantId()))
                .toList();
    }

    private List<AiRecommendationItemView> recommend(List<MenuItem> candidateItems,
                                                     Map<UUID, Restaurant> activeRestaurants,
                                                     Map<UUID, BigDecimal> distanceByRestaurant,
                                                     Map<UUID, Double> vectorSimilarityByMenuItem,
                                                     List<String> tokens,
                                                     String prompt,
                                                     RecommendationIntent intent,
                                                     WeatherSignal weatherSignal) {
        String loweredPrompt = prompt.toLowerCase(Locale.ROOT);
        return candidateItems.stream()
                .sorted(Comparator
                    .comparingInt((MenuItem item) -> score(item,
                        tokens,
                        loweredPrompt,
                        activeRestaurants,
                        distanceByRestaurant,
                        vectorSimilarityByMenuItem,
                        intent,
                        weatherSignal)).reversed()
                        .thenComparing(MenuItem::getName)
                )
                .limit(TOP_K_RECOMMENDATIONS)
                .map(item -> {
                    Restaurant restaurant = activeRestaurants.get(item.getRestaurantId());
                    BigDecimal distanceKm = distanceByRestaurant.get(item.getRestaurantId());
                    return new AiRecommendationItemView(
                            item.getId(),
                            item.getName(),
                            item.getRestaurantId(),
                            restaurant.getName(),
                            item.getPrice(),
                            distanceKm,
                            restaurant.getAvgRating(),
                            reason(item, loweredPrompt, distanceKm, restaurant.getAvgRating(), weatherSignal, intent)
                    );
                })
                .toList();
    }

    private int score(MenuItem item,
                      List<String> tokens,
                      String loweredPrompt,
                      Map<UUID, Restaurant> activeRestaurants,
                      Map<UUID, BigDecimal> distanceByRestaurant,
                      Map<UUID, Double> vectorSimilarityByMenuItem,
                      RecommendationIntent intent,
                      WeatherSignal weatherSignal) {
        String searchBlob = normalizeForSearch(item.getName() + " " + safe(item.getDescription()));
        int score = 0;
        for (String token : tokens) {
            if (searchBlob.contains(token)) {
                score += 4;
            }
        }
        if (searchBlob.contains(normalizeForSearch(loweredPrompt))) {
            score += 8;
        }

        if (intent.requireNotSpicy() && !isLikelySpicy(item)) {
            score += 10;
        } else if (intent.preferSpicy() && isLikelySpicy(item)) {
            score += 10;
        }

        if (intent.requireVegetarian() && isLikelyVegetarian(item)) {
            score += 12;
        }

        if (!intent.requiredProteins().isEmpty() && hasAnyToken(item, intent.requiredProteins())) {
            score += 9;
        }

        if (priceWithinIntent(item.getPrice(), intent)) {
            score += 9;
        } else {
            score -= 12;
        }

        Double vectorSimilarity = vectorSimilarityByMenuItem.get(item.getId());
        if (vectorSimilarity != null) {
            score += (int) Math.round(vectorSimilarity * 35);
        }

        if (intent.requireWarm() != null) {
            boolean warm = isLikelyWarm(item);
            if (intent.requireWarm() && warm) {
                score += 8;
            }
            if (!intent.requireWarm() && !warm) {
                score += 5;
            }
        }

        if (matchesMealType(intent, item)) {
            score += 5;
        }

        if (weatherSignal == WeatherSignal.RAINY || weatherSignal == WeatherSignal.COLD) {
            if (isLikelyWarm(item)) {
                score += 6;
            }
        } else if (weatherSignal == WeatherSignal.HOT) {
            if (isLikelyCold(item)) {
                score += 6;
            }
        }

        Restaurant restaurant = activeRestaurants.get(item.getRestaurantId());
        if (restaurantMatchesRating(restaurant, intent)) {
            score += 8;
        } else {
            score -= 10;
        }
        if (restaurant != null && restaurant.getAvgRating() != null) {
            score += restaurant.getAvgRating().multiply(BigDecimal.valueOf(2)).intValue();
        }

        BigDecimal distance = distanceByRestaurant.get(item.getRestaurantId());
        if (distance != null) {
            score += Math.max(0, 10 - distance.intValue());
        }
        return score;
    }

    private String reason(MenuItem item,
                          String loweredPrompt,
                          BigDecimal distanceKm,
                          BigDecimal restaurantRating,
                          WeatherSignal weatherSignal,
                          RecommendationIntent intent) {
        List<String> reasons = new ArrayList<>();
        String loweredName = item.getName().toLowerCase(Locale.ROOT);
        if (normalizeForSearch(loweredName).contains(normalizeForSearch(loweredPrompt))) {
            reasons.add("keyword match");
        }
        if (distanceKm != null) {
            reasons.add("nearby " + distanceKm.setScale(2, RoundingMode.HALF_UP) + " km");
        }
        if ((weatherSignal == WeatherSignal.RAINY || weatherSignal == WeatherSignal.COLD) && isLikelyWarm(item)) {
            reasons.add("fits cool or rainy weather");
        }
        if (weatherSignal == WeatherSignal.HOT && isLikelyCold(item)) {
            reasons.add("fits hot weather");
        }
        if (isLikelyVegetarian(item)) {
            reasons.add("vegetarian friendly");
        }
        if (priceWithinIntent(item.getPrice(), intent) && (intent.minPrice() != null || intent.maxPrice() != null)) {
            reasons.add("fits your budget");
        }
        if (intent.minRestaurantRating() != null
            && restaurantRating != null
            && restaurantRating.compareTo(intent.minRestaurantRating()) >= 0) {
            reasons.add("meets your rating threshold");
        }
        if (reasons.isEmpty()) {
            reasons.add("relevant internal option");
        }
        return String.join(", ", reasons);
    }

    private Map<UUID, Restaurant> activeRestaurantsById() {
        return restaurantPort.findAll().stream()
                .filter(restaurant -> restaurant.getStatus() == RestaurantStatus.ACTIVE)
                .collect(LinkedHashMap::new, (map, restaurant) -> map.put(restaurant.getId(), restaurant), Map::putAll);
    }

    private Map<UUID, MenuItem> activeMenuItemsById(Collection<UUID> activeRestaurantIds) {
        Map<UUID, MenuItem> result = new LinkedHashMap<>();
        for (MenuItem item : menuItemPort.findByActiveTrueAndAvailableTrueAndDeletedAtIsNull()) {
            if (activeRestaurantIds.contains(item.getRestaurantId())) {
                result.put(item.getId(), item);
            }
        }
        return result;
    }

    private Map<UUID, Double> retrieveVectorSimilarity(String prompt,
                                                       String conversationContext,
                                                       Map<UUID, Restaurant> activeRestaurants,
                                                       Map<UUID, MenuItem> activeMenuItemsById) {
        if (!aiCatalogVectorPort.isReady()) {
            return Map.of();
        }

        try {
            ensureRagSnapshot(activeRestaurants, activeMenuItemsById);
            String queryText = prompt + "\nConversation: " + conversationContext;
            List<Double> queryEmbedding = aiEmbeddingPort.embedText(queryText);
            if (queryEmbedding.isEmpty()) {
                return Map.of();
            }

            Map<UUID, Double> similarityByMenu = new LinkedHashMap<>();
            List<AiCatalogVectorHit> hits = aiCatalogVectorPort.searchByEmbedding(queryEmbedding, RAG_TOP_K);
            for (AiCatalogVectorHit hit : hits) {
                if (!activeMenuItemsById.containsKey(hit.menuItemId())) {
                    continue;
                }
                similarityByMenu.merge(hit.menuItemId(), hit.similarity(), (a, b) -> a > b ? a : b);
            }
            return similarityByMenu;
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private void ensureRagSnapshot(Map<UUID, Restaurant> activeRestaurants,
                                   Map<UUID, MenuItem> activeMenuItemsById) {
        Instant now = Instant.now();
        long chunkCount = aiCatalogVectorPort.countChunks();
        if (chunkCount == activeMenuItemsById.size()
                && lastRagSnapshotAt != null
                && now.isBefore(lastRagSnapshotAt.plusSeconds(RAG_REFRESH_SECONDS))) {
            return;
        }

        List<AiCatalogChunkDocument> chunks = new ArrayList<>();
        for (MenuItem item : activeMenuItemsById.values()) {
            Restaurant restaurant = activeRestaurants.get(item.getRestaurantId());
            if (restaurant == null) {
                continue;
            }
            String chunkText = buildCatalogChunk(item, restaurant);
            List<Double> embedding = aiEmbeddingPort.embedText(chunkText);
            if (embedding.isEmpty()) {
                continue;
            }
            chunks.add(new AiCatalogChunkDocument(
                    item.getId(),
                    item.getRestaurantId(),
                    chunkText,
                    EMPTY_JSON,
                    embedding
            ));
        }

        if (!chunks.isEmpty()) {
            aiCatalogVectorPort.replaceSnapshot(chunks);
            lastRagSnapshotAt = now;
        }
    }

    private static String buildCatalogChunk(MenuItem item, Restaurant restaurant) {
        return "menu_item=" + safe(item.getName())
                + " | description=" + safe(item.getDescription())
                + " | price=" + item.getPrice()
                + " | restaurant=" + safe(restaurant.getName())
                + " | cuisine=" + safe(restaurant.getCuisineType())
                + " | rating=" + safeDecimal(restaurant.getAvgRating())
                + " | open=" + restaurant.isOpen();
    }

    private Map<UUID, BigDecimal> computeDistanceByRestaurant(Map<UUID, Restaurant> activeRestaurants,
                                                              BigDecimal lat,
                                                              BigDecimal lng,
                                                              BigDecimal maxShippingDistanceKm) {
        if (lat == null || lng == null) {
            return Map.of();
        }

        Map<UUID, BigDecimal> distances = new LinkedHashMap<>();
        for (Restaurant restaurant : activeRestaurants.values()) {
            BigDecimal km = geoPort.haversineKm(
                    lat.doubleValue(),
                    lng.doubleValue(),
                    restaurant.getLatitude().doubleValue(),
                    restaurant.getLongitude().doubleValue()
            );
            if (km.compareTo(maxShippingDistanceKm) <= 0) {
                distances.put(restaurant.getId(), km);
            }
        }
        return distances;
    }

    private WeatherContext resolveWeatherContext(BigDecimal lat, BigDecimal lng) {
        if (lat == null || lng == null) {
            return WeatherContext.empty();
        }

        String weatherH3Key = h3Core.geoToH3Address(lat.doubleValue(), lng.doubleValue(), H3_WEATHER_RES);
        Instant now = Instant.now();
        CachedWeather cached = weatherCache.get(weatherH3Key);
        if (cached != null && now.isBefore(cached.expiresAt())) {
            return toWeatherContext(cached.rawWeather(), weatherH3Key);
        }

        try {
            String raw = weatherContextPort.getCurrentWeatherRaw(lat.doubleValue(), lng.doubleValue());
            weatherCache.put(weatherH3Key, new CachedWeather(raw, now.plusSeconds(WEATHER_CACHE_SECONDS)));
            return toWeatherContext(raw, weatherH3Key);
        } catch (Exception ex) {
            return new WeatherContext(null, weatherH3Key, WeatherSignal.UNKNOWN, "not available");
        }
    }

    private WeatherContext toWeatherContext(String raw, String h3) {
        String summary = extractWeatherSummary(raw);
        WeatherSignal signal = resolveWeatherSignal(raw);
        return new WeatherContext(raw, h3, signal, summary);
    }

    private String generateAiDraft(String prompt,
                                   WeatherContext weather,
                                   List<AiRecommendationItemView> recommendations,
                                   RecommendationIntent intent,
                                   BigDecimal maxShippingDistanceKm,
                                   String conversationContext) {
        String candidateText = recommendations.stream()
                .map(item -> item.menuItemName() + " @ " + item.restaurantName()
                        + " (distanceKm=" + safeDecimal(item.distanceKm())
                        + ", rating=" + safeDecimal(item.restaurantRating()) + ")")
                .reduce((a, b) -> a + ", " + b)
                .orElse("none");

        String weatherText = weather.summary();
        String composedPrompt = "User request: " + prompt
            + "\nRecent chat context: " + conversationContext
                + "\nWeather context: " + weatherText
                + "\nIntent hints: " + intent
                + "\nMax shipping distance km: " + maxShippingDistanceKm
                + "\nCatalog candidates: " + candidateText
                + "\nReturn plain text only, concise summary, internal catalog only.";

        try {
            return aiDraftPort.generateRecommendationDraft(composedPrompt);
        } catch (Exception ex) {
            return null;
        }
    }

    private String buildSummary(String aiDraft, List<AiRecommendationItemView> recommendations, RecommendationIntent intent) {
        String draftText = extractModelText(aiDraft);
        if (draftText != null && !draftText.isBlank()) {
            return truncate(draftText.replaceAll("\\s+", " ").trim(), 400);
        }

        if (intent.nearbyRestaurantQuery() && !recommendations.isEmpty()) {
            String nearby = recommendations.stream()
                    .map(item -> item.restaurantName() + " (" + safeDecimal(item.distanceKm()) + " km)")
                    .distinct()
                    .limit(3)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("none");
            return truncate("Nearby restaurants within delivery range: " + nearby, 400);
        }

        if (recommendations.isEmpty()) {
            return "No matching menu items were found within active restaurants and shipping distance.";
        }
        String joined = recommendations.stream()
                .map(item -> item.menuItemName() + " @ " + item.restaurantName())
                .reduce((a, b) -> a + ", " + b)
                .orElse("items");
        return truncate("Recommended from active internal catalog: " + joined, 400);
    }

    private List<String> tokenize(String prompt) {
        String[] raw = TOKEN_SPLITTER.split(prompt.toLowerCase(Locale.ROOT));
        List<String> tokens = new ArrayList<>();
        for (String token : raw) {
            if (token.length() >= 2 && !tokens.contains(token)) {
                tokens.add(token);
            }
            if (tokens.size() >= 12) {
                break;
            }
        }
        return tokens;
    }

    private RecommendationIntent parseIntent(String prompt, List<String> tokens) {
        String rawIntentText = normalizeForIntent(prompt);
        String normalizedPrompt = normalizeForSearch(prompt);
        Set<String> tokenSet = tokens.stream().map(AiRecommendationService::normalizeForSearch).collect(HashSet::new, Set::add, Set::addAll);

        boolean nearby = containsAny(normalizedPrompt, NEARBY_HINTS) || containsAny(tokenSet, NEARBY_HINTS);
        boolean notSpicy = containsAny(normalizedPrompt, NOT_SPICY_HINTS);
        boolean spicy = !notSpicy && containsAny(normalizedPrompt, SPICY_HINTS);
        Boolean warmPreference = null;
        if (containsAny(normalizedPrompt, WARM_HINTS)) {
            warmPreference = true;
        }
        if (containsAny(normalizedPrompt, COLD_HINTS)) {
            warmPreference = false;
        }

        boolean vegetarian = containsAny(normalizedPrompt, VEGETARIAN_HINTS);

        Set<String> proteins = new HashSet<>();
        for (String token : tokenSet) {
            if (PROTEIN_HINTS.contains(token)) {
                proteins.add(token);
            }
        }

        MealTypePreference mealType = MealTypePreference.ANY;
        if (containsAny(normalizedPrompt, SNACK_HINTS)) {
            mealType = MealTypePreference.SNACK;
        } else if (containsAny(normalizedPrompt, MAIN_MEAL_HINTS)) {
            mealType = MealTypePreference.MAIN;
        } else if (containsAny(normalizedPrompt, FAST_FOOD_HINTS)) {
            mealType = MealTypePreference.FAST_FOOD;
        }

        PriceRange budget = extractPriceRange(rawIntentText);
        if (budget == null && containsAny(rawIntentText, BUDGET_HINTS)) {
            budget = new PriceRange(null, CHEAP_PRICE_MAX);
        }

        Integer peopleCount = extractPeopleCount(rawIntentText);
        if (budget != null && peopleCount != null && peopleCount > 1 && !containsAny(rawIntentText, TOTAL_BUDGET_HINTS)) {
            budget = new PriceRange(
                    budget.minPrice() == null ? null : budget.minPrice().divide(BigDecimal.valueOf(peopleCount), 0, RoundingMode.HALF_UP),
                    budget.maxPrice() == null ? null : budget.maxPrice().divide(BigDecimal.valueOf(peopleCount), 0, RoundingMode.HALF_UP)
            );
        }

        BigDecimal minRestaurantRating = extractMinRating(rawIntentText);

        BigDecimal minPrice = budget == null ? null : budget.minPrice();
        BigDecimal maxPrice = budget == null ? null : budget.maxPrice();

        return new RecommendationIntent(nearby,
                spicy,
                notSpicy,
                vegetarian,
                proteins,
                warmPreference,
                mealType,
                minPrice,
                maxPrice,
                minRestaurantRating,
                peopleCount);
    }

    private WeatherSignal resolveWeatherSignal(String rawWeather) {
        if (rawWeather == null || rawWeather.isBlank()) {
            return WeatherSignal.UNKNOWN;
        }
        try {
            JsonNode root = objectMapper.readTree(rawWeather);
            String weatherMain = root.path("weather").path(0).path("main").asText("").toLowerCase(Locale.ROOT);
            double tempC = root.path("main").path("temp").asDouble(Double.NaN);
            if (weatherMain.contains("rain") || weatherMain.contains("drizzle") || weatherMain.contains("thunder")) {
                return WeatherSignal.RAINY;
            }
            if (!Double.isNaN(tempC)) {
                if (tempC >= 31) {
                    return WeatherSignal.HOT;
                }
                if (tempC <= 20) {
                    return WeatherSignal.COLD;
                }
            }
            return WeatherSignal.NORMAL;
        } catch (Exception ex) {
            return WeatherSignal.UNKNOWN;
        }
    }

    private String extractWeatherSummary(String rawWeather) {
        if (rawWeather == null || rawWeather.isBlank()) {
            return "not available";
        }
        try {
            JsonNode root = objectMapper.readTree(rawWeather);
            String weatherMain = root.path("weather").path(0).path("main").asText("unknown");
            String weatherDesc = root.path("weather").path(0).path("description").asText("");
            JsonNode tempNode = root.path("main").path("temp");
            String temp = tempNode.isNumber() ? tempNode.decimalValue().setScale(1, RoundingMode.HALF_UP) + "C" : "n/a";
            String summary = weatherMain + (weatherDesc.isBlank() ? "" : " (" + weatherDesc + ")") + ", temp=" + temp;
            return truncate(summary, 120);
        } catch (Exception ex) {
            return "not available";
        }
    }

    private String extractModelText(String aiDraftRaw) {
        if (aiDraftRaw == null || aiDraftRaw.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(aiDraftRaw);
            JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
            if (parts.isArray() && !parts.isEmpty()) {
                String text = parts.get(0).path("text").asText("");
                if (!text.isBlank()) {
                    return text;
                }
            }
        } catch (Exception ignored) {
            // Adapter may already return plain text, keep fallback below.
        }
        return aiDraftRaw;
    }

    private BigDecimal maxShippingDistanceKm() {
        return systemParameterPort.findById("shipping.max_delivery_km")
                .map(SystemParameter::getValue)
                .map(BigDecimal::new)
                .orElse(BigDecimal.valueOf(DEFAULT_MAX_SHIPPING_KM));
    }

    private static boolean containsAny(String text, Set<String> hints) {
        for (String hint : hints) {
            if (text.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    private static boolean priceWithinIntent(BigDecimal price, RecommendationIntent intent) {
        if (price == null) {
            return false;
        }
        if (intent.minPrice() != null && price.compareTo(intent.minPrice()) < 0) {
            return false;
        }
        if (intent.maxPrice() != null && price.compareTo(intent.maxPrice()) > 0) {
            return false;
        }
        return true;
    }

    private static PriceRange extractPriceRange(String rawIntentText) {
        Matcher rangeMatcher = PRICE_RANGE_PATTERN.matcher(rawIntentText);
        if (rangeMatcher.find()) {
            BigDecimal min = parseMoney(rangeMatcher.group(1), rangeMatcher.group(2));
            BigDecimal max = parseMoney(rangeMatcher.group(3), rangeMatcher.group(4));
            if (min != null && max != null) {
                return min.compareTo(max) <= 0 ? new PriceRange(min, max) : new PriceRange(max, min);
            }
        }

        Matcher genericRangeMatcher = PRICE_RANGE_GENERIC_PATTERN.matcher(rawIntentText);
        if (genericRangeMatcher.find()) {
            BigDecimal min = parseMoney(genericRangeMatcher.group(1), genericRangeMatcher.group(2));
            BigDecimal max = parseMoney(genericRangeMatcher.group(3), genericRangeMatcher.group(4));
            if (min != null && max != null) {
                return min.compareTo(max) <= 0 ? new PriceRange(min, max) : new PriceRange(max, min);
            }
        }

        Matcher maxMatcher = PRICE_MAX_PATTERN.matcher(rawIntentText);
        if (maxMatcher.find()) {
            BigDecimal max = parseMoney(maxMatcher.group(1), maxMatcher.group(2));
            if (max != null) {
                return new PriceRange(null, max);
            }
        }

        Matcher minMatcher = PRICE_MIN_PATTERN.matcher(rawIntentText);
        if (minMatcher.find()) {
            BigDecimal min = parseMoney(minMatcher.group(1), minMatcher.group(2));
            if (min != null) {
                return new PriceRange(min, null);
            }
        }

        Matcher approxMatcher = PRICE_APPROX_PATTERN.matcher(rawIntentText);
        if (approxMatcher.find()) {
            BigDecimal value = parseMoney(approxMatcher.group(1), approxMatcher.group(2));
            if (value != null) {
                if (rawIntentText.contains("do lai") || rawIntentText.contains("tro xuong") || rawIntentText.contains("or less")) {
                    return new PriceRange(null, value);
                }
                BigDecimal delta = value.multiply(BigDecimal.valueOf(0.2));
                return new PriceRange(value.subtract(delta), value.add(delta));
            }
        }

        return null;
    }

    private static BigDecimal extractMinRating(String rawIntentText) {
        Matcher ratingMatcher = RATING_MIN_PATTERN.matcher(rawIntentText);
        if (ratingMatcher.find()) {
            BigDecimal value = parseRating(ratingMatcher.group(1));
            return clampRating(value);
        }

        Matcher suffixMatcher = RATING_MIN_SUFFIX_PATTERN.matcher(rawIntentText);
        if (suffixMatcher.find()) {
            BigDecimal value = parseRating(suffixMatcher.group(1));
            return clampRating(value);
        }

        return null;
    }

    private static BigDecimal parseRating(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.replace(',', '.');
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static BigDecimal clampRating(BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.valueOf(5)) > 0) {
            return BigDecimal.valueOf(5);
        }
        return value;
    }

    private static Integer extractPeopleCount(String rawIntentText) {
        Matcher peopleMatcher = PEOPLE_COUNT_PATTERN.matcher(rawIntentText);
        if (!peopleMatcher.find()) {
            return null;
        }
        try {
            return Integer.parseInt(peopleMatcher.group(1));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static BigDecimal parseMoney(String amountRaw, String unitRaw) {
        if (amountRaw == null || amountRaw.isBlank()) {
            return null;
        }

        String digits = amountRaw.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }

        BigDecimal base = new BigDecimal(digits);
        String unit = unitRaw == null ? "" : unitRaw.toLowerCase(Locale.ROOT);

        if (unit.contains("tr") || unit.equals("m")) {
            return base.multiply(BigDecimal.valueOf(1_000_000));
        }
        if (unit.contains("k") || unit.contains("nghin")) {
            return base.multiply(BigDecimal.valueOf(1_000));
        }
        if (base.compareTo(BigDecimal.valueOf(1_000)) < 0) {
            return base.multiply(BigDecimal.valueOf(1_000));
        }
        return base;
    }

    private static boolean containsAny(Set<String> tokens, Set<String> hints) {
        for (String token : tokens) {
            if (hints.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static boolean restaurantMatchesRating(Restaurant restaurant, RecommendationIntent intent) {
        if (intent.minRestaurantRating() == null) {
            return true;
        }
        if (restaurant == null || restaurant.getAvgRating() == null) {
            return false;
        }
        return restaurant.getAvgRating().compareTo(intent.minRestaurantRating()) >= 0;
    }

    private static boolean isLikelySpicy(MenuItem item) {
        String blob = normalizeForSearch(item.getName() + " " + safe(item.getDescription()));
        return containsAny(blob, SPICY_HINTS);
    }

    private static boolean isLikelyVegetarian(MenuItem item) {
        String blob = normalizeForSearch(item.getName() + " " + safe(item.getDescription()));
        if (containsAny(blob, VEGETARIAN_HINTS)) {
            return true;
        }
        return !containsAny(blob, NON_VEGETARIAN_HINTS);
    }

    private static boolean isLikelyWarm(MenuItem item) {
        String blob = normalizeForSearch(item.getName() + " " + safe(item.getDescription()));
        return containsAny(blob, WARM_HINTS);
    }

    private static boolean isLikelyCold(MenuItem item) {
        String blob = normalizeForSearch(item.getName() + " " + safe(item.getDescription()));
        return containsAny(blob, COLD_HINTS);
    }

    private static boolean hasAnyToken(MenuItem item, Set<String> requiredTokens) {
        String blob = normalizeForSearch(item.getName() + " " + safe(item.getDescription()));
        for (String token : requiredTokens) {
            if (blob.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesMealType(RecommendationIntent intent, MenuItem item) {
        if (intent.mealTypePreference() == MealTypePreference.ANY) {
            return false;
        }
        String blob = normalizeForSearch(item.getName() + " " + safe(item.getDescription()));
        return switch (intent.mealTypePreference()) {
            case SNACK -> containsAny(blob, SNACK_HINTS);
            case MAIN -> containsAny(blob, MAIN_MEAL_HINTS);
            case FAST_FOOD -> containsAny(blob, FAST_FOOD_HINTS);
            default -> false;
        };
    }

    private static String normalizeForSearch(String text) {
        if (text == null) {
            return "";
        }
        String lowered = text.toLowerCase(Locale.ROOT);
        String normalized = Normalizer.normalize(lowered, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return normalized.replaceAll("[^\\p{L}\\p{Nd}]+", "");
    }

    private static String normalizeForIntent(String text) {
        if (text == null) {
            return "";
        }
        String lowered = text.toLowerCase(Locale.ROOT);
        String normalized = Normalizer.normalize(lowered, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return normalized.replaceAll("[^\\p{L}\\p{Nd}\\s\\.,-]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String safeDecimal(BigDecimal value) {
        if (value == null) {
            return "n/a";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String recentConversationContext(UUID customerUserId) {
        List<AiChatHistoryModel> recent = aiChatHistoryPort.findByUserIdOrderByCreatedAtDesc(customerUserId)
                .stream()
                .limit(3)
                .toList();
        if (recent.isEmpty()) {
            return "none";
        }

        List<String> lines = new ArrayList<>();
        for (int i = recent.size() - 1; i >= 0; i--) {
            AiChatHistoryModel chat = recent.get(i);
            lines.add("Q: " + truncate(chat.getPrompt(), 120));
            lines.add("A: " + truncate(chat.getResponseSummary(), 180));
        }
        return truncate(String.join(" | ", lines), 700);
    }

    private static String truncate(String raw, int maxLen) {
        if (raw == null) {
            return null;
        }
        if (raw.length() <= maxLen) {
            return raw;
        }
        return raw.substring(0, maxLen);
    }

    private record CachedWeather(String rawWeather, Instant expiresAt) {
    }

    private record WeatherContext(String rawWeather,
                                  String weatherH3IndexRes8,
                                  WeatherSignal signal,
                                  String summary) {

        private static WeatherContext empty() {
            return new WeatherContext(null, null, WeatherSignal.UNKNOWN, "not available");
        }
    }

    private enum WeatherSignal {
        HOT,
        COLD,
        RAINY,
        NORMAL,
        UNKNOWN
    }

    private enum MealTypePreference {
        ANY,
        SNACK,
        MAIN,
        FAST_FOOD
    }

    private record PriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
    }

    private record RecommendationIntent(boolean nearbyRestaurantQuery,
                                        boolean preferSpicy,
                                        boolean requireNotSpicy,
                                        boolean requireVegetarian,
                                        Set<String> requiredProteins,
                                        Boolean requireWarm,
                                        MealTypePreference mealTypePreference,
                                        BigDecimal minPrice,
                                        BigDecimal maxPrice,
                                        BigDecimal minRestaurantRating,
                                        Integer peopleCount) {

        private boolean hasHardConstraints() {
            return requireNotSpicy
                    || requireVegetarian
                    || !requiredProteins.isEmpty()
                    || requireWarm != null
                    || minPrice != null
                    || maxPrice != null
                    || minRestaurantRating != null;
        }
    }
}
