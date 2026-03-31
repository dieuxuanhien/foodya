package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.dto.CreateOrderReviewRequest;
import com.foodya.backend.application.service.OrderReviewService;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.OrderReviewResponse;
import com.foodya.backend.interfaces.rest.mapper.OrderReviewRestMapper;
import com.foodya.backend.interfaces.rest.support.CurrentUser;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/orders")
public class CustomerReviewController {

    private final OrderReviewService orderReviewService;

    public CustomerReviewController(OrderReviewService orderReviewService) {
        this.orderReviewService = orderReviewService;
    }

    @PostMapping("/{orderId}/review")
    public ApiSuccessResponse<OrderReviewResponse> createReview(Authentication authentication,
                                                                @PathVariable UUID orderId,
                                                                @Valid @RequestBody CreateOrderReviewRequest request,
                                                                HttpServletRequest httpServletRequest) {
        OrderReviewResponse data = OrderReviewRestMapper.toResponse(
                orderReviewService.createReview(CurrentUser.userId(authentication), orderId, request.stars(), request.comment())
        );
        return ApiSuccessResponse.of(data, RequestTrace.from(httpServletRequest));
    }
}
