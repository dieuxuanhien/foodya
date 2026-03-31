package com.foodya.backend.application.port.out;

public interface AiDraftPort {

    String generateRecommendationDraft(String prompt);
}
