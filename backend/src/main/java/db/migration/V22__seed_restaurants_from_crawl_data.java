package db.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uber.h3core.H3Core;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Seeds restaurants from crawled Google Maps data and generates basic menu catalogs.
 * Data is normalized to fit Foodya schema and H3 keys are computed with Uber H3.
 */
public class V22__seed_restaurants_from_crawl_data extends BaseJavaMigration {

    private static final String SOURCE_PATH = "db/migration/restaurantsCrawl.json";
    private static final String[] OWNER_IDS = new String[] {
        "22222222-2222-2222-2222-222222222222",
        "33333333-3333-3333-3333-333333333333",
        "44444444-4444-4444-4444-444444444444"
    };

    private static final String RESTAURANT_INSERT_SQL = """
        INSERT INTO restaurants (
            id, owner_user_id, name, cuisine_type, description, address_line,
            latitude, longitude, h3_index_res9, avg_rating, review_count,
            status, is_open, max_delivery_km,
            image_url, cuisine_types, background_image_url, avatar_image_url,
            created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

    private static final String CATEGORY_INSERT_SQL = """
        INSERT INTO menu_categories (
            id, restaurant_id, name, sort_order, is_active, created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

    private static final String MENU_ITEM_INSERT_SQL = """
        INSERT INTO menu_items (
            id, restaurant_id, category_id, name, description, price,
            is_active, is_available, deleted_at, image_url, created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        ObjectMapper mapper = new ObjectMapper();
        H3Core h3Core = H3Core.newInstance();
        Instant now = Instant.now();
        Timestamp nowTs = Timestamp.from(now);

        List<RestaurantSeed> seeds = loadAndNormalize(mapper, h3Core);

        try (PreparedStatement insertRestaurant = connection.prepareStatement(RESTAURANT_INSERT_SQL);
             PreparedStatement insertCategory = connection.prepareStatement(CATEGORY_INSERT_SQL);
             PreparedStatement insertMenuItem = connection.prepareStatement(MENU_ITEM_INSERT_SQL)) {
            for (RestaurantSeed seed : seeds) {
                insertRestaurant(insertRestaurant, seed, nowTs);

                UUID mainsCategoryId = deterministicUuid("cat:mains:" + seed.restaurantId);
                UUID drinksCategoryId = deterministicUuid("cat:drinks:" + seed.restaurantId);

                insertCategory(insertCategory, mainsCategoryId, seed.restaurantId, "Mains", 0, nowTs);
                insertCategory(insertCategory, drinksCategoryId, seed.restaurantId, "Drinks", 1, nowTs);

                List<MenuItemSeed> items = buildMenu(seed, mainsCategoryId, drinksCategoryId);
                for (MenuItemSeed item : items) {
                    insertMenuItem(insertMenuItem, item, seed.imageUrl, nowTs);
                }
            }
        }
    }

    private static List<RestaurantSeed> loadAndNormalize(ObjectMapper mapper, H3Core h3Core) throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(SOURCE_PATH);
        if (stream == null) {
            throw new IllegalStateException("Missing crawl source at classpath: " + SOURCE_PATH);
        }

        JsonNode root = mapper.readTree(stream);
        if (root == null || !root.isArray()) {
            throw new IllegalStateException("Crawl source is not a JSON array: " + SOURCE_PATH);
        }

        List<RestaurantSeed> seeds = new ArrayList<>();
        Set<String> seenPlaceIds = new LinkedHashSet<>();
        int ownerCursor = 0;

        for (JsonNode node : root) {
            String placeId = text(node, "placeId");
            String title = text(node, "title");
            String address = text(node, "address");
            if (isBlank(placeId) || isBlank(title) || isBlank(address)) {
                continue;
            }

            Double lat = decimal(node, "latitude");
            Double lng = decimal(node, "longitude");
            if (lat == null || lng == null || !isValidCoordinate(lat, lng)) {
                continue;
            }

            // Ignore noisy crawl rows outside Vietnam target scope.
            if (!address.toLowerCase(Locale.ROOT).contains("vietnam")) {
                continue;
            }

            String type = text(node, "type");
            String otherTypes = text(node, "otherTypes");
            if (!looksLikeRestaurant(type, otherTypes)) {
                continue;
            }

            if (!seenPlaceIds.add(placeId)) {
                continue;
            }

            String cuisineType = normalizeCuisineType(type, otherTypes);
            String cuisineTypes = normalizeCuisineTypes(type, otherTypes);
            String imageUrl = normalizeUrl(text(node, "thumbnail"));
            String statusText = text(node, "openState");
            boolean isOpen = statusText.toLowerCase(Locale.ROOT).startsWith("open");

            BigDecimal rating = clampRating(decimal(node, "rating"));
            int reviews = nonNegativeInt(number(node, "reviews"));
            BigDecimal maxDeliveryKm = deliveryRadiusByPopularity(reviews);

            String description = buildDescription(type, rating, reviews, statusText);
            String h3 = h3Core.geoToH3Address(lat, lng, 9);

            UUID restaurantId = deterministicUuid("restaurant:" + placeId);
            String ownerUserId = OWNER_IDS[ownerCursor % OWNER_IDS.length];
            ownerCursor++;

            seeds.add(new RestaurantSeed(
                restaurantId,
                ownerUserId,
                trimToLength(title, 180),
                trimToLength(cuisineType, 80),
                description,
                address,
                BigDecimal.valueOf(lat).setScale(7, RoundingMode.HALF_UP),
                BigDecimal.valueOf(lng).setScale(7, RoundingMode.HALF_UP),
                h3,
                rating,
                reviews,
                isOpen,
                maxDeliveryKm,
                imageUrl,
                cuisineTypes
            ));
        }

        return seeds;
    }

    private static List<MenuItemSeed> buildMenu(RestaurantSeed seed, UUID mainsCategoryId, UUID drinksCategoryId) {
        String c = seed.cuisineType;

        String mainA = trimToLength(c + " Signature", 180);
        String mainB = trimToLength("Chef " + c + " Bowl", 180);
        String mainC = trimToLength("Special " + c + " Combo", 180);
        String drinkA = "Iced Lemon Tea";
        String drinkB = "Fresh Juice";

        int base = 59000 + Math.floorMod(seed.restaurantId.hashCode(), 22000);

        List<MenuItemSeed> items = new ArrayList<>();
        items.add(new MenuItemSeed(
            deterministicUuid("item:m1:" + seed.restaurantId),
            seed.restaurantId,
            mainsCategoryId,
            mainA,
            "Top seller generated from crawl seed",
            BigDecimal.valueOf(base).setScale(2, RoundingMode.HALF_UP)
        ));
        items.add(new MenuItemSeed(
            deterministicUuid("item:m2:" + seed.restaurantId),
            seed.restaurantId,
            mainsCategoryId,
            mainB,
            "Recommended house style meal",
            BigDecimal.valueOf(base + 12000L).setScale(2, RoundingMode.HALF_UP)
        ));
        items.add(new MenuItemSeed(
            deterministicUuid("item:m3:" + seed.restaurantId),
            seed.restaurantId,
            mainsCategoryId,
            mainC,
            "Balanced meal combo for one person",
            BigDecimal.valueOf(base + 25000L).setScale(2, RoundingMode.HALF_UP)
        ));
        items.add(new MenuItemSeed(
            deterministicUuid("item:d1:" + seed.restaurantId),
            seed.restaurantId,
            drinksCategoryId,
            drinkA,
            "Refreshment",
            BigDecimal.valueOf(18000).setScale(2, RoundingMode.HALF_UP)
        ));
        items.add(new MenuItemSeed(
            deterministicUuid("item:d2:" + seed.restaurantId),
            seed.restaurantId,
            drinksCategoryId,
            drinkB,
            "Seasonal drink",
            BigDecimal.valueOf(24000).setScale(2, RoundingMode.HALF_UP)
        ));

        return items;
    }

    private static void insertRestaurant(PreparedStatement ps, RestaurantSeed seed, Timestamp ts) throws Exception {
        ps.setObject(1, seed.restaurantId);
        ps.setObject(2, UUID.fromString(seed.ownerUserId));
        ps.setString(3, seed.name);
        ps.setString(4, seed.cuisineType);
        ps.setString(5, seed.description);
        ps.setString(6, seed.addressLine);
        ps.setBigDecimal(7, seed.latitude);
        ps.setBigDecimal(8, seed.longitude);
        ps.setString(9, seed.h3IndexRes9);
        ps.setBigDecimal(10, seed.avgRating);
        ps.setInt(11, seed.reviewCount);
        ps.setString(12, "ACTIVE");
        ps.setBoolean(13, seed.isOpen);
        ps.setBigDecimal(14, seed.maxDeliveryKm);
        ps.setString(15, seed.imageUrl);
        ps.setString(16, seed.cuisineTypes);
        ps.setString(17, seed.imageUrl);
        ps.setString(18, seed.imageUrl);
        ps.setTimestamp(19, ts);
        ps.setTimestamp(20, ts);
        ps.executeUpdate();
    }

    private static void insertCategory(PreparedStatement ps, UUID categoryId, UUID restaurantId, String name,
                                       int sortOrder, Timestamp ts) throws Exception {
        ps.setObject(1, categoryId);
        ps.setObject(2, restaurantId);
        ps.setString(3, name);
        ps.setInt(4, sortOrder);
        ps.setBoolean(5, true);
        ps.setTimestamp(6, ts);
        ps.setTimestamp(7, ts);
        ps.executeUpdate();
    }

    private static void insertMenuItem(PreparedStatement ps, MenuItemSeed item, String imageUrl, Timestamp ts) throws Exception {
        ps.setObject(1, item.itemId);
        ps.setObject(2, item.restaurantId);
        ps.setObject(3, item.categoryId);
        ps.setString(4, item.name);
        ps.setString(5, item.description);
        ps.setBigDecimal(6, item.price);
        ps.setBoolean(7, true);
        ps.setBoolean(8, true);
        ps.setTimestamp(9, null);
        ps.setString(10, imageUrl);
        ps.setTimestamp(11, ts);
        ps.setTimestamp(12, ts);
        ps.executeUpdate();
    }

    private static String normalizeCuisineType(String type, String otherTypes) {
        List<String> raw = splitTypes(otherTypes);
        if (raw.isEmpty()) {
            raw = splitTypes(type);
        }

        for (String token : raw) {
            String lower = token.toLowerCase(Locale.ROOT);
            if (lower.contains("restaurant")) {
                String cleaned = token.replaceAll("(?i)restaurant", "").trim();
                if (!isBlank(cleaned)) {
                    return titleCase(cleaned);
                }
                continue;
            }
            return titleCase(token);
        }

        return "Vietnamese";
    }

    private static String normalizeCuisineTypes(String type, String otherTypes) {
        LinkedHashSet<String> all = new LinkedHashSet<>();
        for (String token : splitTypes(otherTypes)) {
            if (!isBlank(token)) {
                all.add(titleCase(token.replaceAll("(?i)restaurant", "").trim()));
            }
        }
        if (all.isEmpty()) {
            String fallback = normalizeCuisineType(type, otherTypes);
            all.add(fallback);
        }
        all.remove("");
        return String.join(",", all);
    }

    private static List<String> splitTypes(String raw) {
        List<String> out = new ArrayList<>();
        if (isBlank(raw)) {
            return out;
        }
        for (String part : raw.split(",")) {
            String normalized = part == null ? "" : part.trim();
            if (!normalized.isEmpty()) {
                out.add(normalized);
            }
        }
        return out;
    }

    private static String buildDescription(String type, BigDecimal rating, int reviews, String openState) {
        StringBuilder sb = new StringBuilder("Crawled listing");
        if (!isBlank(type)) {
            sb.append(" • ").append(type.trim());
        }
        sb.append(" • rating ").append(rating.toPlainString()).append("/").append("5");
        sb.append(" from ").append(reviews).append(" reviews");
        if (!isBlank(openState)) {
            sb.append(" • ").append(openState.trim());
        }
        return sb.toString();
    }

    private static BigDecimal clampRating(Double value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        double bounded = Math.max(0.0, Math.min(5.0, value));
        return BigDecimal.valueOf(bounded).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal deliveryRadiusByPopularity(int reviews) {
        if (reviews >= 1000) {
            return BigDecimal.valueOf(12.0).setScale(3, RoundingMode.HALF_UP);
        }
        if (reviews >= 300) {
            return BigDecimal.valueOf(10.0).setScale(3, RoundingMode.HALF_UP);
        }
        if (reviews >= 100) {
            return BigDecimal.valueOf(8.0).setScale(3, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(6.0).setScale(3, RoundingMode.HALF_UP);
    }

    private static UUID deterministicUuid(String input) {
        return UUID.nameUUIDFromBytes(input.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean looksLikeRestaurant(String type, String otherTypes) {
        String haystack = (type + " " + otherTypes).toLowerCase(Locale.ROOT);
        return haystack.contains("restaurant");
    }

    private static boolean isValidCoordinate(double lat, double lng) {
        return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }

    private static String normalizeUrl(String url) {
        if (isBlank(url)) {
            return null;
        }
        String trimmed = url.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimToLength(trimmed, 1024);
        }
        return null;
    }

    private static int nonNegativeInt(Double value) {
        if (value == null) {
            return 0;
        }
        return Math.max(0, value.intValue());
    }

    private static Double number(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.doubleValue();
        }
        if (value.isTextual()) {
            try {
                String text = value.asText();
                if (isBlank(text)) {
                    return null;
                }
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Double decimal(JsonNode node, String field) {
        return number(node, field);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return "";
        }
        return value.asText("").trim();
    }

    private static String titleCase(String text) {
        if (isBlank(text)) {
            return "";
        }
        String[] parts = text.toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(parts[i].charAt(0)));
            if (parts[i].length() > 1) {
                sb.append(parts[i].substring(1));
            }
        }
        return sb.toString().trim();
    }

    private static String trimToLength(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record RestaurantSeed(
        UUID restaurantId,
        String ownerUserId,
        String name,
        String cuisineType,
        String description,
        String addressLine,
        BigDecimal latitude,
        BigDecimal longitude,
        String h3IndexRes9,
        BigDecimal avgRating,
        int reviewCount,
        boolean isOpen,
        BigDecimal maxDeliveryKm,
        String imageUrl,
        String cuisineTypes
    ) {
    }

    private record MenuItemSeed(
        UUID itemId,
        UUID restaurantId,
        UUID categoryId,
        String name,
        String description,
        BigDecimal price
    ) {
    }
}
