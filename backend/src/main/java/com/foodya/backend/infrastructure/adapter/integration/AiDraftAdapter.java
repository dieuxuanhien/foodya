package com.foodya.backend.infrastructure.adapter.integration;

import com.foodya.backend.application.ports.out.AiDraftPort;
import com.foodya.backend.infrastructure.integration.GoogleAiStudioAdapter;
import org.springframework.stereotype.Component;

@Component
public class AiDraftAdapter implements AiDraftPort {

    private final GoogleAiStudioAdapter googleAiStudioAdapter;

    public AiDraftAdapter(GoogleAiStudioAdapter googleAiStudioAdapter) {
        this.googleAiStudioAdapter = googleAiStudioAdapter;
    }

    @Override
    public String generateRecommendationDraft(String prompt) {
        return googleAiStudioAdapter.generateRecommendationDraft(prompt);
    }
}
