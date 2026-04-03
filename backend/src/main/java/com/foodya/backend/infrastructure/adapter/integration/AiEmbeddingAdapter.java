package com.foodya.backend.infrastructure.adapter.integration;

import com.foodya.backend.application.ports.out.AiEmbeddingPort;
import com.foodya.backend.infrastructure.integration.GoogleAiStudioAdapter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AiEmbeddingAdapter implements AiEmbeddingPort {

    private final GoogleAiStudioAdapter googleAiStudioAdapter;

    public AiEmbeddingAdapter(GoogleAiStudioAdapter googleAiStudioAdapter) {
        this.googleAiStudioAdapter = googleAiStudioAdapter;
    }

    @Override
    public List<Double> embedText(String text) {
        return googleAiStudioAdapter.embedText(text);
    }
}
