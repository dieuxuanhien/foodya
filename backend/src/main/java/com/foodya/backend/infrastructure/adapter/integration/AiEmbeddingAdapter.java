package com.foodya.backend.infrastructure.adapter.integration;

import com.foodya.backend.application.ports.out.AiEmbeddingPort;
import com.foodya.backend.infrastructure.integration.GoogleAiStudioClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AiEmbeddingAdapter implements AiEmbeddingPort {

    private final GoogleAiStudioClient googleAiStudioClient;

    public AiEmbeddingAdapter(GoogleAiStudioClient googleAiStudioClient) {
        this.googleAiStudioClient = googleAiStudioClient;
    }

    @Override
    public List<Double> embedText(String text) {
        return googleAiStudioClient.embedText(text);
    }
}
