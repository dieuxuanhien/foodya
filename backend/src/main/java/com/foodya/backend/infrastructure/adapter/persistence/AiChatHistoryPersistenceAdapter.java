package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.dto.AiChatHistoryModel;
import com.foodya.backend.application.ports.out.AiChatHistoryPort;
import com.foodya.backend.domain.entities.AiChatHistory;
import com.foodya.backend.infrastructure.repository.AiChatHistoryRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class AiChatHistoryPersistenceAdapter implements AiChatHistoryPort {

    private final AiChatHistoryRepository repository;

    public AiChatHistoryPersistenceAdapter(AiChatHistoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public AiChatHistoryModel save(AiChatHistoryModel chatHistory) {
        AiChatHistory saved = repository.save(Objects.requireNonNull(toEntity(Objects.requireNonNull(chatHistory))));
        return toModel(saved);
    }

    @Override
    public List<AiChatHistoryModel> findByUserIdOrderByCreatedAtDesc(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public long deleteByCreatedAtBefore(OffsetDateTime cutoff) {
        return repository.deleteByCreatedAtBefore(cutoff);
    }

    private AiChatHistory toEntity(AiChatHistoryModel model) {
        AiChatHistory entity = new AiChatHistory();
        entity.setUserId(model.getUserId());
        entity.setPrompt(model.getPrompt());
        entity.setResponseSummary(model.getResponseSummary());
        entity.setContextLatitude(model.getContextLatitude());
        entity.setContextLongitude(model.getContextLongitude());
        entity.setWeatherH3IndexRes8(model.getWeatherH3IndexRes8());
        return entity;
    }

    private AiChatHistoryModel toModel(AiChatHistory entity) {
        AiChatHistoryModel model = new AiChatHistoryModel();
        model.setId(entity.getId());
        model.setUserId(entity.getUserId());
        model.setPrompt(entity.getPrompt());
        model.setResponseSummary(entity.getResponseSummary());
        model.setContextLatitude(entity.getContextLatitude());
        model.setContextLongitude(entity.getContextLongitude());
        model.setWeatherH3IndexRes8(entity.getWeatherH3IndexRes8());
        model.setCreatedAt(entity.getCreatedAt());
        return model;
    }
}
