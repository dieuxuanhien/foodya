package com.foodya.backend.interfaces.rest.mapper;

import com.foodya.backend.application.dto.AiChatHistoryView;
import com.foodya.backend.application.dto.AiChatResponseView;
import com.foodya.backend.application.dto.AiRecommendationItemView;
import com.foodya.backend.interfaces.rest.dto.AiChatHistoryResponse;
import com.foodya.backend.interfaces.rest.dto.AiChatResponse;
import com.foodya.backend.interfaces.rest.dto.AiRecommendationItemResponse;

public final class AiChatApiMapper {

    private AiChatApiMapper() {
    }

    public static AiChatResponse toResponse(AiChatResponseView view) {
        return new AiChatResponse(
                view.chatId(),
                view.prompt(),
                view.responseSummary(),
                view.recommendations().stream().map(AiChatApiMapper::toItem).toList(),
                view.createdAt()
        );
    }

    public static AiChatHistoryResponse toHistory(AiChatHistoryView view) {
        return new AiChatHistoryResponse(
                view.chatId(),
                view.prompt(),
                view.responseSummary(),
                view.createdAt()
        );
    }

    private static AiRecommendationItemResponse toItem(AiRecommendationItemView item) {
        return new AiRecommendationItemResponse(
                item.menuItemId(),
                item.menuItemName(),
                item.restaurantId(),
                item.restaurantName(),
                item.price(),
            item.distanceKm(),
            item.restaurantRating(),
                item.reason()
        );
    }
}
