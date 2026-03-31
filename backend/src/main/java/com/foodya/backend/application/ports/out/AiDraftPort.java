package com.foodya.backend.application.ports.out;

public interface AiDraftPort {

    String generateRecommendationDraft(String prompt);
}
