package com.foodya.backend.infrastructure.integration;

import com.foodya.backend.infrastructure.config.ApiSecretsProvider;
import com.foodya.backend.infrastructure.config.IntegrationKey;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class GoogleAiStudioAdapter {

    private static final String GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com";

    private final RestClient restClient;
    private final ApiSecretsProvider apiSecretsProvider;

    public GoogleAiStudioAdapter(ApiSecretsProvider apiSecretsProvider) {
        this.apiSecretsProvider = apiSecretsProvider;
        this.restClient = RestClient.builder().baseUrl(GEMINI_ENDPOINT).build();
    }

    public String generateRecommendationDraft(String prompt) {
        String apiKey = apiSecretsProvider.get(IntegrationKey.GOOGLE_AI_STUDIO_API_KEY)
                .orElseThrow(() -> new IllegalStateException("Missing Google AI Studio API key"));

        Map<String, Object> payload = Map.of(
                "contents", new Object[]{Map.of("parts", new Object[]{Map.of("text", prompt)})}
        );

        return restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/gemini-1.5-flash:generateContent")
                        .queryParam("key", apiKey)
                        .build())
                .body(payload)
                .retrieve()
                .body(String.class);
    }
}
