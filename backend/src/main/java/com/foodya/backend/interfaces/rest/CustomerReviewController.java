package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.dto.CreateOrderReviewRequest;
import com.foodya.backend.application.ports.in.OrderReviewUseCase;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.OrderReviewResponse;
import com.foodya.backend.interfaces.rest.mapper.OrderReviewApiMapper;
import com.foodya.backend.interfaces.rest.support.CurrentUser;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    private final OrderReviewUseCase orderReviewService;

    public CustomerReviewController(OrderReviewUseCase orderReviewService) {
        this.orderReviewService = orderReviewService;
    }

    @PostMapping("/{orderId}/reviews")
    public ResponseEntity<ApiSuccessResponse<OrderReviewResponse>> createReview(Authentication authentication,
                                                                                 @PathVariable UUID orderId,
                                                                                 @Valid @RequestBody CreateOrderReviewRequest request,
                                                                                 HttpServletRequest httpServletRequest) {
        OrderReviewResponse data = createReviewData(authentication, orderId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.of(data, RequestTrace.from(httpServletRequest)));
    }

    private OrderReviewResponse createReviewData(Authentication authentication,
                                                 UUID orderId,
                                                 CreateOrderReviewRequest request) {
        OrderReviewResponse data = OrderReviewApiMapper.toResponse(
                orderReviewService.createReview(CurrentUser.userId(authentication), orderId, request.stars(), request.comment())
        );
        return data;
    }
}
