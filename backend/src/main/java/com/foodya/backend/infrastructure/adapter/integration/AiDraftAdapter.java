package com.foodya.backend.infrastructure.adapter.integration;

import com.foodya.backend.application.ports.out.AiDraftPort;
import com.foodya.backend.infrastructure.integration.GoogleAiStudioClient;
import org.springframework.stereotype.Component;

@Component
public class AiDraftAdapter implements AiDraftPort {

    private final GoogleAiStudioClient googleAiStudioClient;

    public AiDraftAdapter(GoogleAiStudioClient googleAiStudioClient) {
        this.googleAiStudioClient = googleAiStudioClient;
    }

    @Override
    public String generateRecommendationDraft(String prompt) {
        return googleAiStudioClient.generateRecommendationDraft(prompt);
    }
}
