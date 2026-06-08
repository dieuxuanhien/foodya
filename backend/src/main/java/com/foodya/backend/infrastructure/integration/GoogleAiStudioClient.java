package com.foodya.backend.infrastructure.integration;

import com.foodya.backend.infrastructure.config.ApiSecretsProvider;
import com.foodya.backend.application.constants.IntegrationKeyCatalog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class GoogleAiStudioClient {

    private static final String GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com";
        private static final String GENERATION_MODEL = "gemini-2.5-flash";
        private static final String EMBEDDING_MODEL = "gemini-embedding-001";

        /**
         * The Supabase ai_catalog_chunks.embedding column is provisioned as vector(768);
         * gemini-embedding-001 natively returns 3072-dim vectors, so we ask it to truncate
         * (via Matryoshka representation learning) to 768 to match the stored column width.
         */
        private static final int EMBEDDING_OUTPUT_DIMENSIONS = 768;

    /**
     * Gemini's embedding model quota is ~100 requests/minute; pacing calls at this interval
     * keeps every caller (chat-query embedding and catalog-sync embedding alike, since they
     * share the same quota) under that ceiling instead of bursting into 429s.
     */
    private static final long EMBEDDING_MIN_INTERVAL_MILLIS = 700L;

    private final Object embeddingRateLimitLock = new Object();
    private long lastEmbeddingCallAtMillis;

    private final RestClient restClient;
    private final ApiSecretsProvider apiSecretsProvider;
        private final ObjectMapper objectMapper;

        public GoogleAiStudioClient(ApiSecretsProvider apiSecretsProvider,
                                                                 ObjectMapper objectMapper) {
        this.apiSecretsProvider = apiSecretsProvider;
                this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl(GEMINI_ENDPOINT).build();
    }

    public String generateRecommendationDraft(String prompt) {
        String apiKey = apiSecretsProvider.get(IntegrationKeyCatalog.GOOGLE_AI_STUDIO_API_KEY)
                .orElseThrow(() -> new IllegalStateException("Missing Google AI Studio API key"));

        Map<String, Object> payload = Map.of(
                "contents", new Object[]{Map.of("parts", new Object[]{Map.of("text", prompt)})}
        );

        return restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/" + GENERATION_MODEL + ":generateContent")
                        .queryParam("key", apiKey)
                        .build())
                .body(Objects.requireNonNull(payload))
                .retrieve()
                .body(String.class);
    }

        public List<Double> embedText(String text) {
                String apiKey = apiSecretsProvider.get(IntegrationKeyCatalog.GOOGLE_AI_STUDIO_API_KEY)
                                .orElseThrow(() -> new IllegalStateException("Missing Google AI Studio API key"));

                throttleEmbeddingCall();

                Map<String, Object> payload = Map.of(
                                "model", "models/" + EMBEDDING_MODEL,
                                "content", Map.of("parts", new Object[]{Map.of("text", text)}),
                                "outputDimensionality", EMBEDDING_OUTPUT_DIMENSIONS
                );

                String raw = restClient.post()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/v1beta/models/" + EMBEDDING_MODEL + ":embedContent")
                                                .queryParam("key", apiKey)
                                                .build())
                                .body(Objects.requireNonNull(payload))
                                .retrieve()
                                .body(String.class);

                if (raw == null || raw.isBlank()) {
                        return List.of();
                }

                try {
                        JsonNode values = objectMapper.readTree(raw).path("embedding").path("values");
                        if (!values.isArray() || values.isEmpty()) {
                                return List.of();
                        }
                        List<Double> result = new ArrayList<>(values.size());
                        for (JsonNode node : values) {
                                result.add(node.asDouble());
                        }
                        return result;
                } catch (Exception ex) {
                        return List.of();
                }
        }

        private void throttleEmbeddingCall() {
                synchronized (embeddingRateLimitLock) {
                        long now = System.currentTimeMillis();
                        long waitMillis = lastEmbeddingCallAtMillis + EMBEDDING_MIN_INTERVAL_MILLIS - now;
                        if (waitMillis > 0) {
                                try {
                                        Thread.sleep(waitMillis);
                                } catch (InterruptedException ex) {
                                        Thread.currentThread().interrupt();
                                }
                        }
                        lastEmbeddingCallAtMillis = System.currentTimeMillis();
                }
        }
}
