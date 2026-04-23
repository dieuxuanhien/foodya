package com.foodya.backend.application.ports.in;

import com.foodya.backend.application.dto.AiChatHistoryView;
import com.foodya.backend.application.dto.AiChatResponseView;
import com.foodya.backend.application.dto.CreateAiChatRequest;

import java.util.List;
import java.util.UUID;

public interface AiRecommendationUseCase {

    AiChatResponseView createChat(UUID customerUserId, CreateAiChatRequest request);

    List<AiChatHistoryView> history(UUID customerUserId);
}
