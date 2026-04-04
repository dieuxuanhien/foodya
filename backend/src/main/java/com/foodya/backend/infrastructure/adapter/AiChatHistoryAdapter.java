package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.dto.AiChatHistoryModel;
import com.foodya.backend.application.ports.out.AiChatHistoryPort;
import com.foodya.backend.domain.entities.AiChatHistory;
import com.foodya.backend.infrastructure.mapper.AiChatHistoryMapper;
import com.foodya.backend.infrastructure.repository.AiChatHistoryRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class AiChatHistoryAdapter implements AiChatHistoryPort {

    private final AiChatHistoryRepository repository;
    private final AiChatHistoryMapper mapper;

    public AiChatHistoryAdapter(AiChatHistoryRepository repository, AiChatHistoryMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public AiChatHistoryModel save(AiChatHistoryModel chatHistory) {
        var entity = toEntity(Objects.requireNonNull(chatHistory));
        var model = mapper.toPersistence(entity);
        var saved = repository.save(model);
        return toModel(mapper.toDomain(saved));
    }

    @Override
    public List<AiChatHistoryModel> findByUserIdOrderByCreatedAtDesc(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(mapper::toDomain)
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
