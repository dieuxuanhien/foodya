package com.foodya.backend.infrastructure.adapter.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodya.backend.application.ports.out.AiDraftPort;
import com.foodya.backend.infrastructure.integration.GoogleAiStudioClient;
import org.springframework.stereotype.Component;

@Component
public class AiDraftAdapter implements AiDraftPort {

    private final GoogleAiStudioClient googleAiStudioClient;
    private final ObjectMapper objectMapper;

    public AiDraftAdapter(GoogleAiStudioClient googleAiStudioClient, ObjectMapper objectMapper) {
        this.googleAiStudioClient = googleAiStudioClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String generateRecommendationDraft(String prompt) {
        try {
            String rawJson = googleAiStudioClient.generateRecommendationDraft(prompt);
            if (rawJson == null || rawJson.isBlank()) {
                return null;
            }
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
            if (parts.isArray() && !parts.isEmpty()) {
                String text = parts.get(0).path("text").asText("");
                if (!text.isBlank()) {
                    return text;
                }
            }
            return rawJson;
        } catch (Exception ex) {
            return null;
        }
    }
}
