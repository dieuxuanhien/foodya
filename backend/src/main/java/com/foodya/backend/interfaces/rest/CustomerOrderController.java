package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.dto.CreateOrderRequest;
import com.foodya.backend.application.dto.OrderCreatedView;
import com.foodya.backend.application.ports.in.OrderCheckoutUseCase;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.OrderCreatedResponse;
import com.foodya.backend.interfaces.rest.mapper.OrderApiMapper;
import com.foodya.backend.interfaces.rest.support.CurrentUser;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customer/orders")
@Tag(name = "Customer Orders", description = "Customer checkout and order creation")
public class CustomerOrderController {

    private final OrderCheckoutUseCase orderCheckoutService;

    public CustomerOrderController(OrderCheckoutUseCase orderCheckoutService) {
        this.orderCheckoutService = orderCheckoutService;
    }

    @PostMapping
    @Operation(summary = "Create order", description = "Creates an order with idempotency support")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "422", description = "Validation failed")
    })
    public ResponseEntity<ApiSuccessResponse<OrderCreatedResponse>> createOrder(Authentication authentication,
                                                                                 @RequestHeader("Idempotency-Key") String idempotencyKey,
                                                                                 @Valid @RequestBody CreateOrderRequest request,
                                                                                 HttpServletRequest httpServletRequest) {
        OrderCreatedView view = orderCheckoutService.createOrder(CurrentUser.userId(authentication), idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiSuccessResponse.of(OrderApiMapper.toResponse(view), RequestTrace.from(httpServletRequest)));
    }
}
