package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.dto.CreateAiChatRequest;
import com.foodya.backend.application.usecases.AiRecommendationService;
import com.foodya.backend.interfaces.rest.dto.AiChatHistoryResponse;
import com.foodya.backend.interfaces.rest.dto.AiChatResponse;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.mapper.AiChatRestMapper;
import com.foodya.backend.interfaces.rest.support.CurrentUser;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/ai")
public class CustomerAiContractController {

    private final AiRecommendationService aiRecommendationService;

    public CustomerAiContractController(AiRecommendationService aiRecommendationService) {
        this.aiRecommendationService = aiRecommendationService;
    }

    @PostMapping("/recommendations")
    public ApiSuccessResponse<AiChatResponse> recommendations(Authentication authentication,
                                                              @Valid @RequestBody CreateAiChatRequest request,
                                                              HttpServletRequest httpServletRequest) {
        AiChatResponse data = AiChatRestMapper.toResponse(
                aiRecommendationService.createChat(CurrentUser.userId(authentication), request)
        );
        return ApiSuccessResponse.of(data, RequestTrace.from(httpServletRequest));
    }

    @GetMapping("/suggestions/today")
    public ApiSuccessResponse<List<AiChatHistoryResponse>> suggestionsToday(Authentication authentication,
                                                                             HttpServletRequest httpServletRequest) {
        LocalDate today = LocalDate.now();
        List<AiChatHistoryResponse> data = aiRecommendationService.history(CurrentUser.userId(authentication)).stream()
                .filter(chat -> chat.createdAt() != null && chat.createdAt().toLocalDate().isEqual(today))
                .map(AiChatRestMapper::toHistory)
                .toList();
        return ApiSuccessResponse.of(data, RequestTrace.from(httpServletRequest));
    }
}
