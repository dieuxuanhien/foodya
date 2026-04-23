package com.foodya.backend.infrastructure.adapter.media;

import com.foodya.backend.application.constants.IntegrationKeyCatalog;
import com.foodya.backend.infrastructure.config.ApiSecretsProvider;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@ActiveProfiles("test")
class SupabaseStorageImageUploadIntegrationTests {

    @Autowired
    private LocalMenuItemImageStorageAdapter localMenuItemImageStorageAdapter;

    @Autowired
    private LocalRestaurantImageStorageAdapter localRestaurantImageStorageAdapter;

    @MockitoBean
    private ApiSecretsProvider apiSecretsProvider;

    private HttpServer httpServer;
    private int port;
    private final AtomicInteger putRequestCount = new AtomicInteger(0);
    private final List<String> requestPaths = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        putRequestCount.set(0);
        requestPaths.clear();

        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = httpServer.getAddress().getPort();
        httpServer.createContext("/", new PutAcceptingS3LikeHandler());
        httpServer.start();

        given(apiSecretsProvider.get(anyString())).willAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return switch (key) {
                case IntegrationKeyCatalog.SUPABASE_PROJECT_URL -> Optional.of("http://127.0.0.1:" + port);
                case IntegrationKeyCatalog.SUPABASE_STORAGE_BUCKET -> Optional.of("test-bucket");
                case IntegrationKeyCatalog.SUPABASE_S3_ACCESS_KEY_ID -> Optional.of("access-key");
                case IntegrationKeyCatalog.SUPABASE_S3_SECRET_ACCESS_KEY -> Optional.of("secret-key");
                default -> Optional.empty();
            };
        });
    }

    @AfterEach
    void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Test
    void menuItemStore_uploadsToSupabaseS3EndpointAndReturnsPublicObjectUrl() {
        UUID menuItemId = UUID.randomUUID();

        String url = localMenuItemImageStorageAdapter.store(
                menuItemId,
                "pho.jpg",
                "image/jpeg",
                "binary-image".getBytes(StandardCharsets.UTF_8)
        );

        assertTrue(url.startsWith("http://127.0.0.1:" + port + "/storage/v1/object/public/test-bucket/menu-items/" + menuItemId));
        assertTrue(url.endsWith(".jpg"));

        assertTrue(putRequestCount.get() >= 1);
        assertTrue(requestPaths.stream().anyMatch(path ->
                path.contains("/storage/v1/s3/test-bucket/menu-items/" + menuItemId)));
    }

    @Test
    void restaurantStore_uploadsBackgroundAndAvatarToSupabaseS3Endpoint() {
        UUID restaurantId = UUID.randomUUID();

        String backgroundUrl = localRestaurantImageStorageAdapter.storeBackground(
                restaurantId,
                "bg.png",
                "image/png",
                "background-bytes".getBytes(StandardCharsets.UTF_8)
        );

        String avatarUrl = localRestaurantImageStorageAdapter.storeAvatar(
                restaurantId,
                "avatar.webp",
                "image/webp",
                "avatar-bytes".getBytes(StandardCharsets.UTF_8)
        );

        assertTrue(backgroundUrl.startsWith("http://127.0.0.1:" + port + "/storage/v1/object/public/test-bucket/restaurants/" + restaurantId + "/background/"));
        assertTrue(backgroundUrl.endsWith(".png"));

        assertTrue(avatarUrl.startsWith("http://127.0.0.1:" + port + "/storage/v1/object/public/test-bucket/restaurants/" + restaurantId + "/avatar/"));
        assertTrue(avatarUrl.endsWith(".webp"));

        assertTrue(putRequestCount.get() >= 2);
        assertTrue(requestPaths.stream().anyMatch(path ->
                path.contains("/storage/v1/s3/test-bucket/restaurants/" + restaurantId + "/background/")));
        assertTrue(requestPaths.stream().anyMatch(path ->
                path.contains("/storage/v1/s3/test-bucket/restaurants/" + restaurantId + "/avatar/")));
    }

    private class PutAcceptingS3LikeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try (InputStream ignored = exchange.getRequestBody()) {
                requestPaths.add(exchange.getRequestURI().getPath());
                if ("PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
                    putRequestCount.incrementAndGet();
                    exchange.getResponseHeaders().add("ETag", "\"test-etag\"");
                    exchange.sendResponseHeaders(200, -1);
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            } finally {
                exchange.close();
            }
        }
    }
}
