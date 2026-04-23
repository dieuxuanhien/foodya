package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.dto.AiChatHistoryData;
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
    public AiChatHistoryData save(AiChatHistoryData chatHistory) {
        AiChatHistory saved = mapper.toDomain(repository.save(Objects.requireNonNull(mapper.toPersistence(toEntity(Objects.requireNonNull(chatHistory))))));
        return toData(saved);
    }

    @Override
    public List<AiChatHistoryData> findByUserIdOrderByCreatedAtDesc(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(mapper::toDomain)
                .map(this::toData)
                .toList();
    }

    @Override
    public long deleteByCreatedAtBefore(OffsetDateTime cutoff) {
        return repository.deleteByCreatedAtBefore(cutoff);
    }

    private AiChatHistory toEntity(AiChatHistoryData model) {
        AiChatHistory entity = new AiChatHistory();
        entity.setId(model.getId());
        entity.setUserId(model.getUserId());
        entity.setPrompt(model.getPrompt());
        entity.setResponseSummary(model.getResponseSummary());
        entity.setContextLatitude(model.getContextLatitude());
        entity.setContextLongitude(model.getContextLongitude());
        entity.setWeatherH3IndexRes8(model.getWeatherH3IndexRes8());
        return entity;
    }

    private AiChatHistoryData toData(AiChatHistory entity) {
        AiChatHistoryData data = new AiChatHistoryData();
        data.setId(entity.getId());
        data.setUserId(entity.getUserId());
        data.setPrompt(entity.getPrompt());
        data.setResponseSummary(entity.getResponseSummary());
        data.setContextLatitude(entity.getContextLatitude());
        data.setContextLongitude(entity.getContextLongitude());
        data.setWeatherH3IndexRes8(entity.getWeatherH3IndexRes8());
        data.setCreatedAt(entity.getCreatedAt());
        return data;
    }
}
