package com.foodya.backend.infrastructure.mapper;

import com.foodya.backend.domain.entities.AiChatHistory;
import com.foodya.backend.infrastructure.persistence.models.AiChatHistoryPersistenceModel;
import org.springframework.stereotype.Component;

/**
 * MAPPER: AiChatHistory Domain Entity ↔ AiChatHistoryPersistenceModel
 */
@Component
public class AiChatHistoryMapper {

    public AiChatHistory toDomain(AiChatHistoryPersistenceModel model) {
        if (model == null) {
            return null;
        }

        AiChatHistory domain = new AiChatHistory();
        domain.setId(model.getId());
        domain.setUserId(model.getUserId());
        domain.setPrompt(model.getPrompt());
        domain.setResponseSummary(model.getResponseSummary());
        domain.setContextLatitude(model.getContextLatitude());
        domain.setContextLongitude(model.getContextLongitude());
        domain.setWeatherH3IndexRes8(model.getWeatherH3IndexRes8());
        domain.setCreatedAt(model.getCreatedAt());
        domain.setUpdatedAt(model.getUpdatedAt());

        return domain;
    }

    public AiChatHistoryPersistenceModel toPersistence(AiChatHistory domain) {
        if (domain == null) {
            return null;
        }

        AiChatHistoryPersistenceModel model = new AiChatHistoryPersistenceModel();
        model.setId(domain.getId());
        model.setUserId(domain.getUserId());
        model.setPrompt(domain.getPrompt());
        model.setResponseSummary(domain.getResponseSummary());
        model.setContextLatitude(domain.getContextLatitude());
        model.setContextLongitude(domain.getContextLongitude());
        model.setWeatherH3IndexRes8(domain.getWeatherH3IndexRes8());
        model.setCreatedAt(domain.getCreatedAt());
        model.setUpdatedAt(domain.getUpdatedAt());

        return model;
    }
}
