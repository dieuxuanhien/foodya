package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.dto.UserAccountModel;
import com.foodya.backend.application.ports.out.TokenPort;
import com.foodya.backend.domain.value_objects.UserRole;
import com.foodya.backend.domain.value_objects.UserStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OpenApiLiveRouteSmokeIntegrationTests {

        private static final String PASSWORD = "Strong@123";
        private static final String SEEDED_ADMIN_USER_ID = "12121212-1212-1212-1212-121212121212";
        private static final String SEEDED_MERCHANT_USER_ID = "13131313-1313-1313-1313-131313131313";
        private static final String SEEDED_DELIVERY_USER_ID = "14141414-1414-1414-1414-141414141414";
        private static final String SEEDED_CUSTOMER_USER_ID = "15151515-1515-1515-1515-151515151515";
    private static final String SEEDED_RESTAURANT_ID = "16161616-1616-1616-1616-161616161616";
    private static final String SEEDED_CATEGORY_ID = "17171717-1717-1717-1717-171717171717";
    private static final String SEEDED_MENU_ITEM_ID = "18181818-1818-1818-1818-181818181818";
    private static final String SEEDED_ACCEPTED_ORDER_ID = "3c3c3c3c-3c3c-3c3c-3c3c-3c3c3c3c3c3c";
    private static final String SEEDED_ASSIGNED_ORDER_ID = "2b2b2b2b-2b2b-2b2b-2b2b-2b2b2b2b2b2b";
    private static final String SEEDED_SUCCESS_ORDER_ID = "1a1a1a1a-1a1a-1a1a-1a1a-1a1a1a1a1a1a";
    private static final String SEEDED_REVIEW_ID = "84848484-8484-8484-8484-848484848484";
    private static final String SEEDED_NOTIFICATION_ID = "90909090-9090-9090-9090-909090909090";

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

        @Autowired
        private TokenPort tokenPort;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Test
    void shouldInvokeEveryDocumentedRouteOnLiveServer() throws Exception {
        JsonNode openApi = request("GET", "/v3/api-docs", null, null).bodyJson;
        Set<String> expectedOperations = extractOperations(openApi);

        AuthContext auth = authenticateSeedUsers();

        Set<String> executedOperations = new LinkedHashSet<>();
        for (String operation : expectedOperations) {
                        try {
                                executeOperation(operation, auth);
                        } catch (AssertionError ex) {
                                fail("Operation failed: " + operation + " -> " + ex.getMessage());
                        }
            executedOperations.add(operation);
        }

        Set<String> missing = new LinkedHashSet<>(expectedOperations);
        missing.removeAll(executedOperations);
        assertTrue(missing.isEmpty(), "OpenAPI operations missing smoke calls: " + missing);
    }

    private void executeOperation(String operation, AuthContext auth) throws Exception {
        String randomId = UUID.randomUUID().toString();
        switch (operation) {
            case "GET /health/live" -> assertNot5xx(request("GET", "/health/live", null, null));
            case "GET /health/ready" -> assertNot5xx(request("GET", "/health/ready", null, null));

            case "POST /api/v1/auth/register" -> {
                String suffix = UUID.randomUUID().toString().substring(0, 8);
                String body = "{" +
                        "\"username\":\"smoke-" + suffix + "\"," +
                        "\"email\":\"smoke-" + suffix + "@example.com\"," +
                        "\"phoneNumber\":\"+8492" + suffix.substring(0, 6) + "\"," +
                        "\"fullName\":\"Smoke User\"," +
                        "\"password\":\"Strong@123\"," +
                        "\"role\":\"CUSTOMER\"}";
                assertNot5xx(request("POST", "/api/v1/auth/register", null, body));
            }
            case "POST /api/v1/auth/login" -> assertNot5xx(request("POST", "/api/v1/auth/login", null,
                    "{\"usernameOrEmail\":\"api_customer\",\"password\":\"" + PASSWORD + "\"}"));
            case "POST /api/v1/auth/refresh" -> assertNot5xx(request("POST", "/api/v1/auth/refresh", null,
                    "{\"refreshToken\":\"" + auth.customerRefresh + "\"}"));
            case "POST /api/v1/auth/forgot-password" -> assertNot5xx(request("POST", "/api/v1/auth/forgot-password", null,
                    "{\"email\":\"api_customer@foodya.local\"}"));
            case "POST /api/v1/auth/forgot-password/verify-otp" -> assertNot5xx(request("POST", "/api/v1/auth/forgot-password/verify-otp", null,
                    "{\"challengeToken\":\"invalid\",\"otp\":\"000000\"}"));
            case "POST /api/v1/auth/reset-password" -> assertNot5xx(request("POST", "/api/v1/auth/reset-password", null,
                    "{\"resetToken\":\"invalid\",\"newPassword\":\"Strong@1234\",\"confirmPassword\":\"Strong@1234\"}"));
            case "POST /api/v1/auth/logout" -> assertNot5xx(request("POST", "/api/v1/auth/logout", auth.customerAccess,
                    "{\"refreshToken\":\"" + auth.customerRefresh + "\"}"));
            case "POST /api/v1/auth/logout-all" -> assertNot5xx(request("POST", "/api/v1/auth/logout-all", auth.customerAccess, "{}"));

            case "GET /api/v1/me" -> assertNot5xx(request("GET", "/api/v1/me", auth.customerAccess, null));
            case "PATCH /api/v1/me" -> assertNot5xx(request("PATCH", "/api/v1/me", auth.customerAccess,
                    "{\"fullName\":\"API Customer Updated\"}"));
            case "PUT /api/v1/me/password" -> assertNot5xx(request("PUT", "/api/v1/me/password", auth.customerAccess,
                    "{\"currentPassword\":\"Strong@123\",\"newPassword\":\"Strong@123!\",\"confirmPassword\":\"Strong@123!\"}"));

            case "GET /api/v1/restaurants" -> assertNot5xx(request("GET", "/api/v1/restaurants?page=0&size=5", null, null));
            case "GET /api/v1/restaurants/nearby" -> assertNot5xx(request("GET", "/api/v1/restaurants/nearby?lat=10.77&lng=106.70&radiusKm=0&page=0&size=5", null, null));
            case "GET /api/v1/restaurants/{id}" -> assertNot5xx(request("GET", "/api/v1/restaurants/" + SEEDED_RESTAURANT_ID, null, null));
            case "GET /api/v1/restaurants/{id}/menu-items" -> assertNot5xx(request("GET", "/api/v1/restaurants/" + SEEDED_RESTAURANT_ID + "/menu-items", null, null));
            case "GET /api/v1/restaurants/{restaurantId}/reviews" -> assertNot5xx(request("GET", "/api/v1/restaurants/" + SEEDED_RESTAURANT_ID + "/reviews", null, null));

            case "GET /api/v1/customer/carts/active" -> assertNot5xx(request("GET", "/api/v1/customer/carts/active", auth.customerAccess, null));
            case "POST /api/v1/customer/carts/active/items" -> assertNot5xx(request("POST", "/api/v1/customer/carts/active/items", auth.customerAccess,
                    "{\"menuItemId\":\"" + SEEDED_MENU_ITEM_ID + "\",\"quantity\":1}"));
            case "PATCH /api/v1/customer/carts/active/items/{menuItemId}" -> assertNot5xx(request("PATCH", "/api/v1/customer/carts/active/items/" + SEEDED_MENU_ITEM_ID, auth.customerAccess,
                    "{\"quantity\":2}"));
            case "DELETE /api/v1/customer/carts/active/items/{menuItemId}" -> assertNot5xx(request("DELETE", "/api/v1/customer/carts/active/items/" + SEEDED_MENU_ITEM_ID, auth.customerAccess, null));
            case "DELETE /api/v1/customer/carts/active/items" -> assertNot5xx(request("DELETE", "/api/v1/customer/carts/active/items", auth.customerAccess, null));

            case "POST /api/v1/customer/orders" -> assertNot5xx(requestWithHeaders("POST", "/api/v1/customer/orders", auth.customerAccess,
                    "{}", Map.of("Idempotency-Key", "smoke-idem-" + UUID.randomUUID())));
            case "GET /api/v1/customer/orders" -> assertNot5xx(request("GET", "/api/v1/customer/orders", auth.customerAccess, null));
            case "GET /api/v1/customer/orders/{orderId}" -> assertNot5xx(request("GET", "/api/v1/customer/orders/" + SEEDED_SUCCESS_ORDER_ID, auth.customerAccess, null));
            case "POST /api/v1/customer/orders/{orderId}/cancel" -> assertNot5xx(request("POST", "/api/v1/customer/orders/" + SEEDED_ASSIGNED_ORDER_ID + "/cancel", auth.customerAccess,
                    "{\"reason\":\"smoke cancel\"}"));
            case "GET /api/v1/customer/orders/{orderId}/tracking" -> assertNot5xx(request("GET", "/api/v1/customer/orders/" + SEEDED_ASSIGNED_ORDER_ID + "/tracking", auth.customerAccess, null));
            case "POST /api/v1/customer/orders/{orderId}/reviews" -> assertNot5xx(request("POST", "/api/v1/customer/orders/" + SEEDED_SUCCESS_ORDER_ID + "/reviews", auth.customerAccess,
                    "{\"stars\":5,\"comment\":\"smoke review\"}"));

            case "POST /api/v1/customer/ai/chats" -> assertNot5xx(request("POST", "/api/v1/customer/ai/chats", auth.customerAccess,
                    "{\"prompt\":\"\",\"latitude\":10.77,\"longitude\":106.70}"));
            case "GET /api/v1/customer/ai/chats" -> assertNot5xx(request("GET", "/api/v1/customer/ai/chats", auth.customerAccess, null));

            case "GET /api/v1/notifications" -> assertNot5xx(request("GET", "/api/v1/notifications", auth.customerAccess, null));
            case "PATCH /api/v1/notifications/{id}/read" -> assertNot5xx(request("PATCH", "/api/v1/notifications/" + SEEDED_NOTIFICATION_ID + "/read", auth.customerAccess, "{}"));

            case "POST /api/v1/merchant/restaurants" -> assertNot5xx(request("POST", "/api/v1/merchant/restaurants", auth.merchantAccess,
                    "{\"name\":\"Smoke Merchant Rest\",\"cuisineType\":\"Vietnamese\",\"description\":\"smoke\",\"addressLine\":\"1 smoke road\",\"latitude\":10.77,\"longitude\":106.70,\"maxDeliveryKm\":5.0}"));
            case "PATCH /api/v1/merchant/restaurants/{id}" -> assertNot5xx(request("PATCH", "/api/v1/merchant/restaurants/" + SEEDED_RESTAURANT_ID, auth.merchantAccess,
                    "{\"name\":\"API Smoke Bistro Updated\",\"isOpen\":true}"));
            case "POST /api/v1/merchant/restaurants/{id}/menu-categories" -> assertNot5xx(request("POST", "/api/v1/merchant/restaurants/" + SEEDED_RESTAURANT_ID + "/menu-categories", auth.merchantAccess,
                    "{\"name\":\"Smoke Cat\",\"sortOrder\":2}"));
            case "GET /api/v1/merchant/restaurants/{id}/menu-categories" -> assertNot5xx(request("GET", "/api/v1/merchant/restaurants/" + SEEDED_RESTAURANT_ID + "/menu-categories", auth.merchantAccess, null));
            case "PATCH /api/v1/merchant/menu-categories/{id}" -> assertNot5xx(request("PATCH", "/api/v1/merchant/menu-categories/" + SEEDED_CATEGORY_ID, auth.merchantAccess,
                    "{\"name\":\"Seed Main Updated\",\"sortOrder\":1}"));
            case "DELETE /api/v1/merchant/menu-categories/{id}" -> assertNot5xx(request("DELETE", "/api/v1/merchant/menu-categories/" + randomId, auth.merchantAccess, null));
            case "POST /api/v1/merchant/restaurants/{id}/menu-items" -> assertNot5xx(request("POST", "/api/v1/merchant/restaurants/" + SEEDED_RESTAURANT_ID + "/menu-items", auth.merchantAccess,
                    "{\"categoryId\":\"" + SEEDED_CATEGORY_ID + "\",\"name\":\"Smoke New Item\",\"description\":\"smoke\",\"price\":55000}"));
            case "GET /api/v1/merchant/restaurants/{id}/menu-items" -> assertNot5xx(request("GET", "/api/v1/merchant/restaurants/" + SEEDED_RESTAURANT_ID + "/menu-items", auth.merchantAccess, null));
            case "PATCH /api/v1/merchant/menu-items/{id}" -> assertNot5xx(request("PATCH", "/api/v1/merchant/menu-items/" + SEEDED_MENU_ITEM_ID, auth.merchantAccess,
                    "{\"name\":\"Seed Noodles Updated\",\"price\":92000}"));
            case "DELETE /api/v1/merchant/menu-items/{id}" -> assertNot5xx(request("DELETE", "/api/v1/merchant/menu-items/" + randomId, auth.merchantAccess, null));
            case "PATCH /api/v1/merchant/menu-items/{id}/availability" -> assertNot5xx(request("PATCH", "/api/v1/merchant/menu-items/" + SEEDED_MENU_ITEM_ID + "/availability", auth.merchantAccess,
                    "{\"isAvailable\":true}"));

            case "GET /api/v1/merchant/restaurants/{restaurantId}/orders" -> assertNot5xx(request("GET", "/api/v1/merchant/restaurants/" + SEEDED_RESTAURANT_ID + "/orders", auth.merchantAccess, null));
            case "PATCH /api/v1/merchant/orders/{orderId}/status" -> assertNot5xx(request("PATCH", "/api/v1/merchant/orders/" + SEEDED_ASSIGNED_ORDER_ID + "/status", auth.merchantAccess,
                    "{\"status\":\"PREPARING\"}"));

            case "PATCH /api/v1/merchant/reviews/{reviewId}/response" -> assertNot5xx(request("PATCH", "/api/v1/merchant/reviews/" + SEEDED_REVIEW_ID + "/response", auth.merchantAccess,
                    "{\"response\":\"Thanks from smoke suite\"}"));
            case "POST /api/v1/merchant/reviews/{reviewId}/replies" -> assertNot5xx(request("POST", "/api/v1/merchant/reviews/" + SEEDED_REVIEW_ID + "/replies", auth.merchantAccess,
                    "{\"response\":\"Smoke reply\"}"));
            case "PATCH /api/v1/merchant/review-replies/{id}" -> assertNot5xx(request("PATCH", "/api/v1/merchant/review-replies/" + randomId, auth.merchantAccess,
                    "{\"response\":\"Smoke update\"}"));

            case "GET /api/v1/delivery/orders/assignments" -> assertNot5xx(request("GET", "/api/v1/delivery/orders/assignments", auth.deliveryAccess, null));
            case "GET /api/v1/delivery/orders/assigned" -> assertNot5xx(request("GET", "/api/v1/delivery/orders/assigned", auth.deliveryAccess, null));
            case "POST /api/v1/delivery/orders/{orderId}/accept" -> assertNot5xx(request("POST", "/api/v1/delivery/orders/" + SEEDED_ACCEPTED_ORDER_ID + "/accept", auth.deliveryAccess, "{}"));
            case "PATCH /api/v1/delivery/orders/{orderId}/status" -> assertNot5xx(request("PATCH", "/api/v1/delivery/orders/" + SEEDED_ASSIGNED_ORDER_ID + "/status", auth.deliveryAccess,
                    "{\"status\":\"DELIVERING\"}"));
            case "POST /api/v1/delivery/orders/{orderId}/tracking-points" -> assertNot5xx(request("POST", "/api/v1/delivery/orders/" + SEEDED_ASSIGNED_ORDER_ID + "/tracking-points", auth.deliveryAccess,
                    "{\"lat\":10.776,\"lng\":106.701,\"recordedAt\":\"2026-03-31T10:00:00Z\"}"));
            case "POST /api/v1/delivery/orders/{orderId}/locations" -> assertNot5xx(request("POST", "/api/v1/delivery/orders/" + SEEDED_ASSIGNED_ORDER_ID + "/locations", auth.deliveryAccess,
                    "{\"lat\":10.777,\"lng\":106.702,\"recordedAt\":\"2026-03-31T10:05:00Z\"}"));

            case "GET /api/v1/admin/restaurants" -> assertNot5xx(request("GET", "/api/v1/admin/restaurants", auth.adminAccess, null));
            case "POST /api/v1/admin/restaurants/{id}/approve" -> assertNot5xx(request("POST", "/api/v1/admin/restaurants/" + SEEDED_RESTAURANT_ID + "/approve", auth.adminAccess, "{}"));
            case "POST /api/v1/admin/restaurants/{id}/reject" -> assertNot5xx(request("POST", "/api/v1/admin/restaurants/" + SEEDED_RESTAURANT_ID + "/reject", auth.adminAccess, "{}"));
            case "DELETE /api/v1/admin/restaurants/{id}" -> assertNot5xx(request("DELETE", "/api/v1/admin/restaurants/" + SEEDED_RESTAURANT_ID, auth.adminAccess, null));
            case "GET /api/v1/admin/orders" -> assertNot5xx(request("GET", "/api/v1/admin/orders", auth.adminAccess, null));
            case "PATCH /api/v1/admin/orders/{id}/status" -> assertNot5xx(request("PATCH", "/api/v1/admin/orders/" + SEEDED_ACCEPTED_ORDER_ID + "/status", auth.adminAccess,
                    "{\"status\":\"ASSIGNED\"}"));
            case "DELETE /api/v1/admin/orders/{id}" -> assertNot5xx(request("DELETE", "/api/v1/admin/orders/" + randomId, auth.adminAccess, null));
            case "DELETE /api/v1/admin/menu-items/{id}" -> assertNot5xx(request("DELETE", "/api/v1/admin/menu-items/" + randomId, auth.adminAccess, null));

            case "GET /api/v1/admin/users" -> assertNot5xx(request("GET", "/api/v1/admin/users", auth.adminAccess, null));
            case "POST /api/v1/admin/users/{id}/lock" -> assertNot5xx(request("POST", "/api/v1/admin/users/" + auth.customerUserId + "/lock", auth.adminAccess, "{}"));
            case "POST /api/v1/admin/users/{id}/unlock" -> assertNot5xx(request("POST", "/api/v1/admin/users/" + auth.customerUserId + "/unlock", auth.adminAccess, "{}"));
            case "DELETE /api/v1/admin/users/{id}" -> assertNot5xx(request("DELETE", "/api/v1/admin/users/" + randomId, auth.adminAccess, null));

            case "GET /api/v1/admin/notifications" -> assertNot5xx(request("GET", "/api/v1/admin/notifications", auth.adminAccess, null));
            case "GET /api/v1/admin/system-parameters" -> assertNot5xx(request("GET", "/api/v1/admin/system-parameters", auth.adminAccess, null));
            case "PUT /api/v1/admin/system-parameters/{key}" -> assertNot5xx(request("PUT", "/api/v1/admin/system-parameters/search.default_page_size", auth.adminAccess,
                    "{\"valueType\":\"NUMBER\",\"paramValue\":\"20\",\"runtimeApplicable\":true,\"description\":\"smoke set\"}"));
            case "PATCH /api/v1/admin/system-parameters/{key}" -> assertNot5xx(request("PATCH", "/api/v1/admin/system-parameters/search.default_page_size", auth.adminAccess,
                    "{\"paramValue\":\"21\"}"));

            case "GET /api/v1/admin/reports/revenue" -> assertNot5xx(request("GET", "/api/v1/admin/reports/revenue?period=MONTHLY", auth.adminAccess, null));
            case "GET /api/v1/merchant/reports/revenue" -> assertNot5xx(request("GET", "/api/v1/merchant/reports/revenue?period=MONTHLY", auth.merchantAccess, null));

            case "GET /api/v1/system/integrations/firebase-config" -> assertNot5xx(request("GET", "/api/v1/system/integrations/firebase-config", auth.adminAccess, null));
            case "GET /api/v1/system/integrations/supabase-config" -> assertNot5xx(request("GET", "/api/v1/system/integrations/supabase-config", auth.adminAccess, null));
            case "GET /api/v1/system/integrations/status" -> assertNot5xx(request("GET", "/api/v1/system/integrations/status", auth.adminAccess, null));

            default -> throw new IllegalStateException("Unhandled OpenAPI operation: " + operation);
        }
    }

    private AuthContext authenticateSeedUsers() throws Exception {
                TokenPair admin = issueSeedToken(UUID.fromString(SEEDED_ADMIN_USER_ID), "api_admin", UserRole.ADMIN);
                TokenPair merchant = issueSeedToken(UUID.fromString(SEEDED_MERCHANT_USER_ID), "api_merchant", UserRole.MERCHANT);
                TokenPair delivery = issueSeedToken(UUID.fromString(SEEDED_DELIVERY_USER_ID), "api_delivery", UserRole.DELIVERY);
                TokenPair customer = issueSeedToken(UUID.fromString(SEEDED_CUSTOMER_USER_ID), "api_customer", UserRole.CUSTOMER);
        return new AuthContext(
                admin.accessToken,
                merchant.accessToken,
                delivery.accessToken,
                customer.accessToken,
                customer.refreshToken,
                                SEEDED_CUSTOMER_USER_ID
        );
    }

    private TokenPair login(String username) throws Exception {
        Response response = request("POST", "/api/v1/auth/login", null,
                "{\"usernameOrEmail\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}");
        assertTrue(response.statusCode < 500, "Login crashed for " + username + " with status " + response.statusCode);
                String access = response.bodyJson == null ? "" : response.bodyJson.path("data").path("accessToken").asText("");
                String refresh = response.bodyJson == null ? "" : response.bodyJson.path("data").path("refreshToken").asText("");
        return new TokenPair(access, refresh);
    }

        private TokenPair issueSeedToken(UUID userId, String username, UserRole role) {
                UserAccountModel user = new UserAccountModel();
                user.setId(userId);
                user.setUsername(username);
                user.setEmail(username + "@foodya.local");
                user.setPhoneNumber("+84900000000");
                user.setFullName(username);
                user.setRole(role);
                user.setStatus(UserStatus.ACTIVE);
                String access = tokenPort.issueAccessToken(user, UUID.randomUUID().toString());
                String refresh = tokenPort.issueRefreshToken(user, UUID.randomUUID().toString(), UUID.randomUUID().toString());
                assertNotNull(access, "Missing access token for " + username);
                assertNotNull(refresh, "Missing refresh token for " + username);
                return new TokenPair(access, refresh);
        }

    private Response request(String method, String path, String bearerToken, String jsonBody) throws IOException, InterruptedException {
                return requestWithHeaders(method, path, bearerToken, jsonBody, Map.of());
        }

        private Response requestWithHeaders(String method,
                                                                                String path,
                                                                                String bearerToken,
                                                                                String jsonBody,
                                                                                Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json");

        if (bearerToken != null && !bearerToken.isBlank()) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        if (jsonBody != null) {
            builder.header("Content-Type", "application/json");
        }
                headers.forEach(builder::header);

        HttpRequest.BodyPublisher publisher = jsonBody == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(jsonBody);

        HttpRequest request = builder.method(method.toUpperCase(Locale.ROOT), publisher).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode json = null;
        if (response.body() != null && !response.body().isBlank() && response.body().trim().startsWith("{")) {
            json = objectMapper.readTree(response.body());
        }
                return new Response(response.statusCode(), response.body(), json);
    }

    private static Set<String> extractOperations(JsonNode openApi) {
        Set<String> operations = new LinkedHashSet<>();
        JsonNode paths = openApi.path("paths");
        paths.fieldNames().forEachRemaining(path -> {
            JsonNode pathNode = paths.path(path);
            pathNode.fieldNames().forEachRemaining(method -> {
                String m = method.toUpperCase(Locale.ROOT);
                if (Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD", "TRACE").contains(m)) {
                    operations.add(m + " " + path);
                }
            });
        });
        assertFalse(operations.isEmpty(), "No operations found in /v3/api-docs");
        return operations;
    }

    private static void assertNot5xx(Response response) {
        assertTrue(response.statusCode < 500,
                "Route invocation returned 5xx: " + response.statusCode + " body=" + response.body);
    }

    private record TokenPair(String accessToken, String refreshToken) {
    }

    private record AuthContext(String adminAccess,
                               String merchantAccess,
                               String deliveryAccess,
                               String customerAccess,
                               String customerRefresh,
                               String customerUserId) {
    }

    private static final class Response {
        private final int statusCode;
        private final String body;
        private final JsonNode bodyJson;

        private Response(int statusCode, String body, JsonNode bodyJson) {
            this.statusCode = statusCode;
            this.body = body;
            this.bodyJson = bodyJson;
        }
    }
}
