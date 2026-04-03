package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.ports.in.OrderLifecycleUseCase;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.CancelOrderRestRequest;
import com.foodya.backend.interfaces.rest.dto.OrderDetailResponse;
import com.foodya.backend.interfaces.rest.dto.OrderSummaryResponse;
import com.foodya.backend.interfaces.rest.dto.OrderTrackingPointResponse;
import com.foodya.backend.interfaces.rest.mapper.OrderLifecycleRestMapper;
import com.foodya.backend.interfaces.rest.support.CurrentUser;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/orders")
public class CustomerOrderLifecycleController {

    private final OrderLifecycleUseCase orderLifecycleService;

    public CustomerOrderLifecycleController(OrderLifecycleUseCase orderLifecycleService) {
        this.orderLifecycleService = orderLifecycleService;
    }

    @GetMapping
    public ApiSuccessResponse<List<OrderSummaryResponse>> myOrders(Authentication authentication,
                                                                   HttpServletRequest request) {
        List<OrderSummaryResponse> data = orderLifecycleService.customerOrders(CurrentUser.userId(authentication))
                .stream()
                .map(OrderLifecycleRestMapper::toSummary)
                .toList();
        return ApiSuccessResponse.of(data, RequestTrace.from(request));
    }

    @GetMapping("/{orderId}")
    public ApiSuccessResponse<OrderDetailResponse> myOrderDetail(Authentication authentication,
                                                                 @PathVariable UUID orderId,
                                                                 HttpServletRequest request) {
        OrderDetailResponse data = OrderLifecycleRestMapper.toDetail(
                orderLifecycleService.customerOrder(CurrentUser.userId(authentication), orderId)
        );
        return ApiSuccessResponse.of(data, RequestTrace.from(request));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiSuccessResponse<OrderDetailResponse> cancelOrder(Authentication authentication,
                                                               @PathVariable UUID orderId,
                                                               @RequestBody(required = false) CancelOrderRestRequest cancelRequest,
                                                               HttpServletRequest request) {
        String reason = cancelRequest == null ? null : cancelRequest.reason();
        OrderDetailResponse data = OrderLifecycleRestMapper.toDetail(
                orderLifecycleService.cancelOrder(CurrentUser.userId(authentication), orderId, reason)
        );
        return ApiSuccessResponse.of(data, RequestTrace.from(request));
    }

    @GetMapping("/{orderId}/tracking")
    public ApiSuccessResponse<List<OrderTrackingPointResponse>> trackingPoints(Authentication authentication,
                                                                               @PathVariable UUID orderId,
                                                                               HttpServletRequest request) {
        List<OrderTrackingPointResponse> data = orderLifecycleService.customerTrackingPoints(CurrentUser.userId(authentication), orderId)
                .stream()
                .map(OrderLifecycleRestMapper::toTrackingPoint)
                .toList();
        return ApiSuccessResponse.of(data, RequestTrace.from(request));
    }
}
