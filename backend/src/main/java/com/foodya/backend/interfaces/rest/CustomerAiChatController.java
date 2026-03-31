package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.dto.CreateAiChatRequest;
import com.foodya.backend.application.service.AiRecommendationService;
import com.foodya.backend.interfaces.rest.dto.AiChatHistoryResponse;
import com.foodya.backend.interfaces.rest.dto.AiChatResponse;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.mapper.AiChatRestMapper;
import com.foodya.backend.interfaces.rest.support.CurrentUser;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/ai/chats")
@Tag(name = "Customer AI", description = "Customer contextual recommendation chat")
public class CustomerAiChatController {

    private final AiRecommendationService aiRecommendationService;

    public CustomerAiChatController(AiRecommendationService aiRecommendationService) {
        this.aiRecommendationService = aiRecommendationService;
    }

    @PostMapping
    @Operation(summary = "Create AI recommendation chat", description = "Returns recommendations from internal active catalog only")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recommendation generated"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "422", description = "Validation failed")
    })
    public ApiSuccessResponse<AiChatResponse> create(Authentication authentication,
                                                     @Valid @RequestBody CreateAiChatRequest request,
                                                     HttpServletRequest httpServletRequest) {
        AiChatResponse data = AiChatRestMapper.toResponse(
                aiRecommendationService.createChat(CurrentUser.userId(authentication), request)
        );
        return ApiSuccessResponse.of(data, RequestTrace.from(httpServletRequest));
    }

    @GetMapping
    @Operation(summary = "List AI chat history", description = "Returns customer chat history ordered by latest first")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "History listed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ApiSuccessResponse<List<AiChatHistoryResponse>> history(Authentication authentication,
                                                                   HttpServletRequest httpServletRequest) {
        List<AiChatHistoryResponse> data = aiRecommendationService.history(CurrentUser.userId(authentication))
                .stream()
                .map(AiChatRestMapper::toHistory)
                .toList();
        return ApiSuccessResponse.of(data, RequestTrace.from(httpServletRequest));
    }
}
