package com.foodya.backend.application.usecases;

import com.foodya.backend.application.dto.AiChatHistoryView;
import com.foodya.backend.application.dto.AiChatResponseView;
import com.foodya.backend.application.dto.AiCatalogChunkDocument;
import com.foodya.backend.application.dto.AiCatalogVectorHit;
import com.foodya.backend.application.dto.AiRecommendationItemView;
import com.foodya.backend.application.dto.CreateAiChatRequest;
import com.foodya.backend.application.dto.AiChatHistoryData;
import com.foodya.backend.application.exception.NotFoundException;
import com.foodya.backend.application.ports.in.AiRecommendationUseCase;
import com.foodya.backend.application.ports.out.AiCatalogVectorPort;
import com.foodya.backend.application.ports.out.AiChatHistoryPort;
import com.foodya.backend.application.ports.out.AiDraftPort;
import com.foodya.backend.application.ports.out.AiEmbeddingPort;
import com.foodya.backend.application.ports.out.GeoPort;
import com.foodya.backend.application.ports.out.MenuItemPort;
import com.foodya.backend.application.ports.out.RestaurantPort;
import com.foodya.backend.application.ports.out.UserAccountPort;
import com.foodya.backend.application.ports.out.SystemParameterPort;
import com.foodya.backend.application.ports.out.WeatherContextPort;
import com.foodya.backend.domain.entities.SystemParameter;
import com.foodya.backend.domain.value_objects.RestaurantStatus;
import com.foodya.backend.domain.entities.MenuItem;
import com.foodya.backend.domain.entities.Restaurant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uber.h3core.H3Core;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiRecommendationService implements AiRecommendationUseCase {

    private static final Logger log = LoggerFactory.getLogger(AiRecommendationService.class);

    private static final int H3_WEATHER_RES = 8;
    private static final int TOP_K_RECOMMENDATIONS = 5;
    private static final int WEATHER_CACHE_SECONDS = 600;
    private static final int RAG_TOP_K = 40;
    private static final double RRF_K = 60.0;
    private static final int DEFAULT_MAX_SHIPPING_KM = 15;
    private static final int MODEL_INTRO_MAX_CHARS = 300;
    private static final int RESPONSE_SUMMARY_MAX_CHARS = 400;
    private static final Pattern TOKEN_SPLITTER = Pattern.compile("[^\\p{L}\\p{Nd}]+");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d");
    private static final Pattern MODEL_ENTITY_SUGGESTION_PATTERN =
            Pattern.compile("(?i)\\b(try|order|choose|pick|go for|recommend|suggest)\\b.+");

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
    private final UserAccountPort userAccountPort;
    private final AiDraftPort aiDraftPort;
    private final WeatherContextPort weatherContextPort;
    private final AiChatHistoryPort aiChatHistoryPort;
    private final SystemParameterPort systemParameterPort;
    private final GeoPort geoPort;
    private final H3Core h3Core;
    private final ChatIntentPlanner chatIntentPlanner;
    private final ObjectMapper objectMapper;
    private final Map<String, CachedWeather> weatherCache = new ConcurrentHashMap<>();

    public AiRecommendationService(AiEmbeddingPort aiEmbeddingPort,
                                   AiCatalogVectorPort aiCatalogVectorPort,
                                   MenuItemPort menuItemPort,
                                   RestaurantPort restaurantPort,
                                   UserAccountPort userAccountPort,
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
                    this.userAccountPort = userAccountPort;
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
        this.chatIntentPlanner = new ChatIntentPlanner(aiEmbeddingPort);
    }

    public AiChatResponseView createChat(UUID customerUserId, CreateAiChatRequest request) {
        userAccountPort.findById(customerUserId)
            .orElseThrow(() -> new NotFoundException("user not found"));
        String normalizedPrompt = request.prompt().trim();
        List<String> tokens = tokenize(normalizedPrompt);
        String conversationContext = recentConversationContext(customerUserId);
        List<Double> promptEmbedding = embedPromptForPlanning(normalizedPrompt, conversationContext);
        ChatIntentCategory intentCategory = chatIntentPlanner.classify(promptEmbedding);

        if (intentCategory == ChatIntentCategory.GENERAL_CHAT) {
            String reply = generateChitchatReply(normalizedPrompt, conversationContext);
            return saveLightweightChat(customerUserId, request, normalizedPrompt, reply);
        }
        if (intentCategory == ChatIntentCategory.OUT_OF_SCOPE) {
            String reply = buildOutOfScopeReply(normalizedPrompt);
            return saveLightweightChat(customerUserId, request, normalizedPrompt, reply);
        }

        RecommendationIntent intent = parseIntent(normalizedPrompt, tokens);
        Map<UUID, Restaurant> activeRestaurants = activeRestaurantsById();
        Map<UUID, MenuItem> activeMenuItemsById = activeMenuItemsById(activeRestaurants.keySet());
        WeatherContext weather = resolveWeatherContext(request.lat(), request.lng());
        BigDecimal maxShippingDistanceKm = maxShippingDistanceKm();
        boolean locationProvided = request.lat() != null && request.lng() != null;
        Map<UUID, Double> vectorSimilarityByMenuItem = retrieveVectorSimilarity(
            normalizedPrompt,
            promptEmbedding,
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
        String responseSummary = buildSafeResponseSummary(aiDraft, recommendations, intent, normalizedPrompt);

        AiChatHistoryData history = new AiChatHistoryData();
        history.setUserId(customerUserId);
        history.setPrompt(normalizedPrompt);
        history.setResponseSummary(responseSummary);
        history.setContextLatitude(request.lat());
        history.setContextLongitude(request.lng());
        history.setWeatherH3IndexRes8(weather.weatherH3IndexRes8());
        AiChatHistoryData saved = aiChatHistoryPort.save(history);

        return new AiChatResponseView(
                saved.getId(),
                saved.getPrompt(),
                saved.getResponseSummary(),
                recommendations,
                saved.getCreatedAt()
        );
    }

    /**
     * Embeds the prompt once up front so the same vector can drive both intent classification
     * and (when the message turns out to be a recommendation request) retrieval — avoiding a
     * second call against the rate-limited embedding model.
     */
    private List<Double> embedPromptForPlanning(String normalizedPrompt, String conversationContext) {
        try {
            String queryText = normalizedPrompt + "\nConversation: " + conversationContext;
            return aiEmbeddingPort.embedText(queryText);
        } catch (Exception ex) {
            return List.of();
        }
    }

    /**
     * Persists chitchat/out-of-scope turns without running weather resolution, candidate scoring,
     * or RAG retrieval — those branches never produce catalog recommendations.
     */
    private AiChatResponseView saveLightweightChat(UUID customerUserId,
                                                    CreateAiChatRequest request,
                                                    String normalizedPrompt,
                                                    String responseSummary) {
        AiChatHistoryData history = new AiChatHistoryData();
        history.setUserId(customerUserId);
        history.setPrompt(normalizedPrompt);
        history.setResponseSummary(responseSummary);
        history.setContextLatitude(request.lat());
        history.setContextLongitude(request.lng());
        history.setWeatherH3IndexRes8(null);
        AiChatHistoryData saved = aiChatHistoryPort.save(history);

        return new AiChatResponseView(
                saved.getId(),
                saved.getPrompt(),
                saved.getResponseSummary(),
                List.of(),
                saved.getCreatedAt()
        );
    }

    public List<AiChatHistoryView> history(UUID customerUserId) {
        return aiChatHistoryPort.findByUserIdOrderByCreatedAtDesc(customerUserId)
                .stream()
                .map(chat -> new AiChatHistoryView(chat.getId(), chat.getPrompt(), chat.getResponseSummary(), chat.getCreatedAt()))
                .toList();
    }

    public void clearHistory(UUID customerUserId) {
        userAccountPort.findById(customerUserId)
                .orElseThrow(() -> new NotFoundException("user not found"));
        aiChatHistoryPort.deleteByUserId(customerUserId);
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
                                                       List<Double> promptEmbedding,
                                                       Map<UUID, Restaurant> activeRestaurants,
                                                       Map<UUID, MenuItem> activeMenuItemsById) {
        if (!aiCatalogVectorPort.isReady() || aiCatalogVectorPort.countChunks() == 0) {
            return Map.of();
        }
        if (promptEmbedding == null || promptEmbedding.isEmpty()) {
            return Map.of();
        }

        try {
            List<AiCatalogVectorHit> vectorHits = aiCatalogVectorPort.searchByEmbedding(promptEmbedding, RAG_TOP_K);
            List<AiCatalogVectorHit> keywordHits = aiCatalogVectorPort.searchByKeyword(prompt, RAG_TOP_K);
            return fuseHybridHits(List.of(vectorHits, keywordHits), activeMenuItemsById);
        } catch (Exception ex) {
            return Map.of();
        }
    }

    /**
     * Merges multiple ranked hit lists (e.g. vector cosine search and keyword full-text search)
     * via Reciprocal Rank Fusion, then min-max normalizes the fused scores to [0,1] so the result
     * stays compatible with score()'s existing Math.round(vectorSimilarity * 35) weighting.
     */
    private Map<UUID, Double> fuseHybridHits(List<List<AiCatalogVectorHit>> rankedHitLists,
                                             Map<UUID, MenuItem> activeMenuItemsById) {
        Map<UUID, Double> fusedByMenuItem = new LinkedHashMap<>();
        for (List<AiCatalogVectorHit> hits : rankedHitLists) {
            for (int rank = 0; rank < hits.size(); rank++) {
                AiCatalogVectorHit hit = hits.get(rank);
                if (!activeMenuItemsById.containsKey(hit.menuItemId())) {
                    continue;
                }
                double rrfScore = 1d / (RRF_K + rank + 1);
                fusedByMenuItem.merge(hit.menuItemId(), rrfScore, Double::sum);
            }
        }

        if (fusedByMenuItem.isEmpty()) {
            return Map.of();
        }

        double maxScore = fusedByMenuItem.values().stream().mapToDouble(Double::doubleValue).max().orElse(0d);
        if (maxScore <= 0d) {
            return Map.of();
        }

        Map<UUID, Double> normalized = new LinkedHashMap<>();
        for (Map.Entry<UUID, Double> entry : fusedByMenuItem.entrySet()) {
            normalized.put(entry.getKey(), entry.getValue() / maxScore);
        }
        return normalized;
    }

    public void refreshCatalogSnapshot() {
        if (!aiCatalogVectorPort.isReady()) {
            return;
        }
        Map<UUID, Restaurant> activeRestaurants = activeRestaurantsById();
        Map<UUID, MenuItem> activeMenuItemsById = activeMenuItemsById(activeRestaurants.keySet());
        syncCatalogSnapshot(activeRestaurants, activeMenuItemsById);
    }

    private void syncCatalogSnapshot(Map<UUID, Restaurant> activeRestaurants,
                                     Map<UUID, MenuItem> activeMenuItemsById) {
        Map<UUID, String> freshChunkTextByMenuItem = new LinkedHashMap<>();
        Map<UUID, String> freshHashByMenuItem = new LinkedHashMap<>();
        for (MenuItem item : activeMenuItemsById.values()) {
            Restaurant restaurant = activeRestaurants.get(item.getRestaurantId());
            if (restaurant == null) {
                continue;
            }
            String chunkText = buildCatalogChunk(item, restaurant);
            freshChunkTextByMenuItem.put(item.getId(), chunkText);
            freshHashByMenuItem.put(item.getId(), sha256Hex(chunkText));
        }

        Map<UUID, String> storedHashByMenuItem = aiCatalogVectorPort.findStoredContentHashes();

        List<UUID> dirtyMenuItemIds = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : freshHashByMenuItem.entrySet()) {
            if (!entry.getValue().equals(storedHashByMenuItem.get(entry.getKey()))) {
                dirtyMenuItemIds.add(entry.getKey());
            }
        }

        Set<UUID> staleMenuItemIds = new HashSet<>(storedHashByMenuItem.keySet());
        staleMenuItemIds.removeAll(activeMenuItemsById.keySet());
        if (!staleMenuItemIds.isEmpty()) {
            aiCatalogVectorPort.deleteChunksForMenuItems(staleMenuItemIds);
            log.info("AI catalog sync: removed {} stale chunk(s)", staleMenuItemIds.size());
        }

        if (dirtyMenuItemIds.isEmpty()) {
            log.info("AI catalog sync: catalog already up to date ({} active item(s), {} stored chunk(s))",
                    activeMenuItemsById.size(), storedHashByMenuItem.size());
            return;
        }

        log.info("AI catalog sync: embedding {} new/changed item(s) out of {} active item(s)",
                dirtyMenuItemIds.size(), activeMenuItemsById.size());

        int embedded = 0;
        for (UUID menuItemId : dirtyMenuItemIds) {
            MenuItem item = activeMenuItemsById.get(menuItemId);
            String chunkText = freshChunkTextByMenuItem.get(menuItemId);
            String freshHash = freshHashByMenuItem.get(menuItemId);
            List<Double> embedding;
            try {
                embedding = aiEmbeddingPort.embedText(chunkText);
            } catch (Exception ex) {
                log.warn("Skipping catalog embedding for menu item {} this cycle (will retry next cycle)", menuItemId, ex);
                continue;
            }
            if (embedding.isEmpty()) {
                continue;
            }
            aiCatalogVectorPort.upsertChunks(List.of(new AiCatalogChunkDocument(
                    menuItemId,
                    item.getRestaurantId(),
                    chunkText,
                    EMPTY_JSON,
                    freshHash,
                    embedding
            )));
            embedded++;
            if (embedded % 25 == 0 || embedded == dirtyMenuItemIds.size()) {
                log.info("AI catalog sync progress: embedded {}/{} item(s)", embedded, dirtyMenuItemIds.size());
            }
        }

        log.info("AI catalog sync completed: embedded {}/{} dirty item(s)", embedded, dirtyMenuItemIds.size());
    }

    private static String sha256Hex(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            return Integer.toHexString(text.hashCode());
        }
    }

    private static String buildCatalogChunk(MenuItem item, Restaurant restaurant) {
        return "menu_item=" + safe(item.getName())
                + " | description=" + safe(item.getDescription())
                + " | price=" + item.getPrice()
                + " | restaurant=" + safe(restaurant.getName())
                + " | cuisine=" + safe(restaurant.getCuisineType());
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

    private static final Pattern VIETNAMESE_HINT_PATTERN = Pattern.compile(
            "(?i)[\\u00e0\\u00e1\\u1ea3\\u00e3\\u1ea1\\u0103\\u1eb1\\u1eaf\\u1eb3\\u1eb5\\u1eb7\\u00e2\\u1ea7\\u1ea5\\u1ea9\\u1eab\\u1ead"
            + "\\u00e8\\u00e9\\u1ebb\\u1ebd\\u1eb9\\u00ea\\u1ec1\\u1ebf\\u1ec3\\u1ec5\\u1ec7"
            + "\\u00ec\\u00ed\\u1ec9\\u0129\\u1ecb"
            + "\\u00f2\\u00f3\\u1ecf\\u00f5\\u1ecd\\u00f4\\u1ed3\\u1ed1\\u1ed5\\u1ed7\\u1ed9\\u01a1\\u1edd\\u1edb\\u1edf\\u1ee1\\u1ee3"
            + "\\u00f9\\u00fa\\u1ee7\\u0169\\u1ee5\\u01b0\\u1eeb\\u1ee9\\u1eed\\u1eef\\u1ef1"
            + "\\u1ef3\\u00fd\\u1ef7\\u1ef9\\u1ef5\\u0111]"
            + "|\\b(ban|toi|minh|gi|la|nhe|voi|mon|an|quan|gan|cay|re|nay|hom|nay)\\b"
    );

    private String generateChitchatReply(String prompt, String conversationContext) {
        String composedPrompt = """
                You are Foodya's friendly food-app assistant.
                The user sent a general conversational message (not a food recommendation request).
                Reply warmly and briefly (1-2 sentences, max 200 characters), matching the user's language
                (default to Vietnamese when unclear).
                Do not mention specific menu items, restaurants, prices, or ratings since you have not searched the catalog.
                Briefly mention that you can help find food and restaurant recommendations on Foodya.
                Return plain text only.
                User message: %s
                Recent chat context: %s
                """.formatted(prompt, conversationContext);

        try {
            String raw = aiDraftPort.generateRecommendationDraft(composedPrompt);
            String text = extractModelText(raw);
            if (text != null && !text.isBlank()) {
                String normalized = text
                        .replaceAll("[\\r\\n]+", " ")
                        .replaceAll("\\s+", " ")
                        .trim();
                normalized = stripWrappingQuotes(normalized);
                if (!normalized.isBlank() && normalized.length() <= RESPONSE_SUMMARY_MAX_CHARS) {
                    return normalized;
                }
            }
        } catch (Exception ex) {
            // fall through to canned reply
        }
        return defaultChitchatReply(prompt);
    }

    private String defaultChitchatReply(String prompt) {
        return looksVietnamese(prompt)
                ? "Mình là trợ lý của Foodya, lúc nào cũng sẵn sàng giúp bạn tìm món ăn và nhà hàng ngon gần đây — cứ hỏi mình nhé!"
                : "I'm Foodya's assistant — I'm here to help you discover great food and restaurants nearby. Just ask away!";
    }

    private String buildOutOfScopeReply(String prompt) {
        return looksVietnamese(prompt)
                ? "Mình là trợ lý gợi ý món ăn của Foodya nên chỉ hỗ trợ các câu hỏi về món ăn, nhà hàng và đặt món thôi nhé — thử hỏi mình \"gợi ý món gần đây\" xem sao!"
                : "I'm Foodya's food recommendation assistant, so I can only help with questions about food, restaurants, and ordering — try asking me to \"suggest something nearby\"!";
    }

    private static boolean looksVietnamese(String text) {
        if (text == null || text.isBlank()) {
            return true;
        }
        return VIETNAMESE_HINT_PATTERN.matcher(text.toLowerCase(Locale.ROOT)).find();
    }

    private String generateAiDraft(String prompt,
                                   WeatherContext weather,
                                   List<AiRecommendationItemView> recommendations,
                                   RecommendationIntent intent,
                                   BigDecimal maxShippingDistanceKm,
                                   String conversationContext) {
        if (recommendations.isEmpty()) {
            return null;
        }

        String composedPrompt = buildRecommendationPrompt(
                prompt,
                weather,
                recommendations,
                intent,
                maxShippingDistanceKm,
                conversationContext
        );

        try {
            return aiDraftPort.generateRecommendationDraft(composedPrompt);
        } catch (Exception ex) {
            return null;
        }
    }

    private String buildRecommendationPrompt(String prompt,
                                             WeatherContext weather,
                                             List<AiRecommendationItemView> recommendations,
                                             RecommendationIntent intent,
                                             BigDecimal maxShippingDistanceKm,
                                             String conversationContext) {
        return """
                You are Foodya's food recommendation assistant.
                Goal: write a warm, PERSONALIZED 2-3 sentence response that directly addresses what the user asked for, then introduces the recommendation cards Foodya is about to show.
                LANGUAGE RULE (highest priority): Detect the language of "User request" below and reply in EXACTLY that language. Vietnamese request → Vietnamese reply. English request → English reply. Mixed → follow the dominant language.
                Content rules:
                - In the first sentence, acknowledge the user's specific intent (e.g. if they asked for something spicy and cheap, say so — but do not invent facts).
                - In the second sentence, briefly explain why the shown results match their request (use the "reason" fields from the catalog candidates as context; do NOT copy the raw reason text — rephrase naturally).
                - In the third sentence, invite them to browse the cards.
                - Do NOT mention or invent menu item names, restaurant names, prices, ratings, distances, promotions, nutrition facts, delivery times, or delivery guarantees.
                - Do NOT say you can order or reserve for the user.
                - Return plain text only. No markdown, no bullets.
                - Maximum 300 characters total.
                User request: %s
                Recent chat context: %s
                Weather context: %s
                Intent hints: %s
                Max shipping distance km: %s
                Foodya catalog candidates (use the "reason" field to understand why each was selected):
                %s
                """.formatted(
                prompt,
                conversationContext,
                weather.summary(),
                intent,
                maxShippingDistanceKm,
                buildCandidateText(recommendations)
        );
    }

    private String buildCandidateText(List<AiRecommendationItemView> recommendations) {
        return recommendations.stream()
                .map(item -> "- menuItem=\"" + item.menuItemName()
                        + "\" restaurant=\"" + item.restaurantName()
                        + "\" distanceKm=" + safeDecimal(item.distanceKm())
                        + " rating=" + safeDecimal(item.restaurantRating())
                        + " reason=\"" + item.reason() + "\"")
                .reduce((a, b) -> a + "\n" + b)
                .orElse("none");
    }

    private String buildSafeResponseSummary(String aiDraft,
                                            List<AiRecommendationItemView> recommendations,
                                            RecommendationIntent intent,
                                            String userPrompt) {
        boolean vi = looksVietnamese(userPrompt);

        if (recommendations.isEmpty()) {
            return vi
                    ? "Mình chưa tìm thấy lựa chọn nào phù hợp lúc này. Bạn thử mở rộng từ khóa, bỏ bớt bộ lọc đánh giá hoặc ngân sách, hoặc đổi vị trí xem nhé!"
                    : "I couldn't find a matching Foodya option right now. Try a broader keyword, lower rating or budget filters, or another location.";
        }

        if (intent.nearbyRestaurantQuery()) {
            String nearby = recommendations.stream()
                    .map(item -> item.restaurantName() + " (" + safeDecimal(item.distanceKm()) + " km)")
                    .distinct()
                    .limit(3)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("none");
            return vi
                    ? truncate("Có một số nhà hàng gần bạn trong vùng giao hàng: " + nearby + ". Chọn thẻ bên dưới để xem thực đơn nhé!", RESPONSE_SUMMARY_MAX_CHARS)
                    : truncate("Nearby restaurants within delivery range: " + nearby + ". Pick a Foodya card below to view the menu.", RESPONSE_SUMMARY_MAX_CHARS);
        }

        String safeIntro = sanitizeModelIntro(aiDraft, recommendations);
        if (safeIntro != null) {
            return truncate(safeIntro, RESPONSE_SUMMARY_MAX_CHARS);
        }

        return vi
                ? "Đây là một số gợi ý từ Foodya phù hợp với yêu cầu của bạn. Chọn thẻ bên dưới để xem chi tiết nhé!"
                : "Here are some Foodya options that match what you're looking for. Pick a card below to see the details!";
    }

    private String sanitizeModelIntro(String aiDraftRaw, List<AiRecommendationItemView> recommendations) {
        String draftText = extractModelText(aiDraftRaw);
        if (draftText == null || draftText.isBlank()) {
            return null;
        }

        String normalized = draftText
                .replaceAll("[\\r\\n]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        normalized = stripWrappingQuotes(normalized);
        if (normalized.isBlank() || normalized.length() > MODEL_INTRO_MAX_CHARS) {
            return null;
        }
        if (DIGIT_PATTERN.matcher(normalized).find()
                || normalized.contains("@")
                || mentionsCatalogEntity(normalized, recommendations)
                || containsUnsupportedClaim(normalized)) {
            return null;
        }
        return normalized;
    }

    private static boolean mentionsCatalogEntity(String text, List<AiRecommendationItemView> recommendations) {
        String normalizedText = normalizeForSearch(text);
        for (AiRecommendationItemView recommendation : recommendations) {
            if (containsNamedEntity(normalizedText, recommendation.menuItemName())
                    || containsNamedEntity(normalizedText, recommendation.restaurantName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsNamedEntity(String normalizedText, String entityName) {
        String normalizedEntity = normalizeForSearch(entityName);
        return normalizedEntity.length() >= 4 && normalizedText.contains(normalizedEntity);
    }

    private static boolean containsUnsupportedClaim(String text) {
        String normalized = normalizeForSearch(text);
        return normalized.contains("promotion")
                || normalized.contains("promo")
                || normalized.contains("discount")
                || normalized.contains("voucher")
                || normalized.contains("nutrition")
                || normalized.contains("calorie")
                || normalized.contains("deliverytime")
                || normalized.contains("guarantee")
                || normalized.contains("reservation");
    }

    private static String stripWrappingQuotes(String text) {
        String result = text;
        while (result.length() >= 2
                && ((result.startsWith("\"") && result.endsWith("\""))
                || (result.startsWith("'") && result.endsWith("'")))) {
            result = result.substring(1, result.length() - 1).trim();
        }
        return result;
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
        List<AiChatHistoryData> recent = aiChatHistoryPort.findByUserIdOrderByCreatedAtDesc(customerUserId)
                .stream()
                .limit(3)
                .toList();
        if (recent.isEmpty()) {
            return "none";
        }

        List<String> lines = new ArrayList<>();
        for (int i = recent.size() - 1; i >= 0; i--) {
            AiChatHistoryData chat = recent.get(i);
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
