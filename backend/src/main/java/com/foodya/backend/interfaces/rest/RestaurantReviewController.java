package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.ports.in.OrderReviewUseCase;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.OrderReviewResponse;
import com.foodya.backend.interfaces.rest.mapper.OrderReviewApiMapper;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/restaurants")
public class RestaurantReviewController {

    private final OrderReviewUseCase orderReviewService;

    public RestaurantReviewController(OrderReviewUseCase orderReviewService) {
        this.orderReviewService = orderReviewService;
    }

    @GetMapping("/{restaurantId}/reviews")
    public ApiSuccessResponse<List<OrderReviewResponse>> reviews(@PathVariable UUID restaurantId,
                                                                 HttpServletRequest httpServletRequest) {
        List<OrderReviewResponse> data = orderReviewService.listRestaurantReviews(restaurantId)
                .stream()
                .map(OrderReviewApiMapper::toResponse)
                .toList();
        return ApiSuccessResponse.of(data, RequestTrace.from(httpServletRequest));
    }
}
