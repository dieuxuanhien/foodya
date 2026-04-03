package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.ports.in.OrderLifecycleUseCase;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.OrderDetailResponse;
import com.foodya.backend.interfaces.rest.dto.OrderStatusUpdateRestRequest;
import com.foodya.backend.interfaces.rest.dto.OrderSummaryResponse;
import com.foodya.backend.interfaces.rest.mapper.OrderLifecycleRestMapper;
import com.foodya.backend.interfaces.rest.support.CurrentUser;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/merchant")
public class MerchantOrderController {

    private final OrderLifecycleUseCase orderLifecycleService;

    public MerchantOrderController(OrderLifecycleUseCase orderLifecycleService) {
        this.orderLifecycleService = orderLifecycleService;
    }

    @GetMapping("/restaurants/{restaurantId}/orders")
    public ApiSuccessResponse<List<OrderSummaryResponse>> restaurantOrders(Authentication authentication,
                                                                           @PathVariable UUID restaurantId,
                                                                           HttpServletRequest request) {
        List<OrderSummaryResponse> data = orderLifecycleService.merchantOrders(CurrentUser.userId(authentication), restaurantId)
                .stream()
                .map(OrderLifecycleRestMapper::toSummary)
                .toList();
        return ApiSuccessResponse.of(data, RequestTrace.from(request));
    }

    @PatchMapping("/orders/{orderId}/status")
    public ApiSuccessResponse<OrderDetailResponse> updateOrderStatus(Authentication authentication,
                                                                     @PathVariable UUID orderId,
                                                                     @Valid @RequestBody OrderStatusUpdateRestRequest statusUpdate,
                                                                     HttpServletRequest request) {
        OrderStatus targetStatus = parseStatus(statusUpdate.status());
        OrderDetailResponse data = OrderLifecycleRestMapper.toDetail(
                orderLifecycleService.merchantUpdateStatus(CurrentUser.userId(authentication), orderId, targetStatus)
        );
        return ApiSuccessResponse.of(data, RequestTrace.from(request));
    }

    private static OrderStatus parseStatus(String rawStatus) {
        try {
            return OrderStatus.valueOf(rawStatus.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("invalid order status", Map.of("status", "unknown status value"));
        }
    }
}
