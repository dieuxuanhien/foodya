package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.dto.RespondOrderReviewRequest;
import com.foodya.backend.application.usecases.OrderReviewService;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.OrderReviewResponse;
import com.foodya.backend.interfaces.rest.mapper.OrderReviewRestMapper;
import com.foodya.backend.interfaces.rest.support.CurrentUser;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/merchant/reviews")
public class MerchantReviewController {

    private final OrderReviewService orderReviewService;

    public MerchantReviewController(OrderReviewService orderReviewService) {
        this.orderReviewService = orderReviewService;
    }

    @PatchMapping("/{reviewId}/response")
    public ApiSuccessResponse<OrderReviewResponse> respond(Authentication authentication,
                                                           @PathVariable UUID reviewId,
                                                           @Valid @RequestBody RespondOrderReviewRequest request,
                                                           HttpServletRequest httpServletRequest) {
        OrderReviewResponse data = OrderReviewRestMapper.toResponse(
                orderReviewService.merchantRespond(CurrentUser.userId(authentication), reviewId, request.response())
        );
        return ApiSuccessResponse.of(data, RequestTrace.from(httpServletRequest));
    }

    @PostMapping("/{reviewId}/replies")
    public ResponseEntity<ApiSuccessResponse<OrderReviewResponse>> reply(Authentication authentication,
                                                                         @PathVariable UUID reviewId,
                                                                         @Valid @RequestBody RespondOrderReviewRequest request,
                                                                         HttpServletRequest httpServletRequest) {
        OrderReviewResponse data = OrderReviewRestMapper.toResponse(
                orderReviewService.merchantRespond(CurrentUser.userId(authentication), reviewId, request.response())
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.of(data, RequestTrace.from(httpServletRequest)));
    }
}
