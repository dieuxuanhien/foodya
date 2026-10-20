package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.ports.in.OrderReviewUseCase;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.OrderReviewResponse;
import com.foodya.backend.interfaces.rest.mapper.OrderReviewApiMapper;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/restaurants")
@Tag(name = "Restaurant Reviews", description = "Public read access to restaurant order reviews")
public class RestaurantReviewController {

    private final OrderReviewUseCase orderReviewService;

    public RestaurantReviewController(OrderReviewUseCase orderReviewService) {
        this.orderReviewService = orderReviewService;
    }

    @GetMapping("/{restaurantId}/reviews")
    @Operation(summary = "List restaurant reviews", description = "Returns published reviews for a restaurant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reviews listed"),
            @ApiResponse(responseCode = "404", description = "Restaurant not found")
    })
    public ResponseEntity<ApiSuccessResponse<List<OrderReviewResponse>>> reviews(@PathVariable UUID restaurantId,
                                                                 HttpServletRequest httpServletRequest) {
        List<OrderReviewResponse> data = orderReviewService.listRestaurantReviews(restaurantId)
                .stream()
                .map(OrderReviewApiMapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiSuccessResponse.of(data, RequestTrace.from(httpServletRequest)));
    }
}
