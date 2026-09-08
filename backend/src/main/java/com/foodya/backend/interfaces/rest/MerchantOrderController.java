package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.ports.in.OrderLifecycleUseCase;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.application.dto.OrderSummaryView;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.OrderDetailResponse;
import com.foodya.backend.interfaces.rest.dto.OrderStatusUpdateApiRequest;
import com.foodya.backend.interfaces.rest.dto.OrderSummaryResponse;
import com.foodya.backend.interfaces.rest.dto.PageMetadata;
import com.foodya.backend.interfaces.rest.mapper.OrderLifecycleApiMapper;
import com.foodya.backend.interfaces.rest.support.CurrentUser;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
                                                                           @RequestParam(required = false) @Min(0) Integer page,
                                                                           @RequestParam(required = false) @Min(1) @Max(200) Integer size,
                                                                           HttpServletRequest request) {
        PaginatedResult<OrderSummaryView> result = orderLifecycleService.merchantOrders(CurrentUser.userId(authentication), restaurantId, page, size);
        List<OrderSummaryResponse> data = result.items().stream()
                .map(OrderLifecycleApiMapper::toSummary)
                .toList();
        return ApiSuccessResponse.of(
                data,
                new PageMetadata(result.page(), result.size(), result.totalElements(), result.totalPages()),
                RequestTrace.from(request)
        );
    }

    @GetMapping("/orders/{orderId}")
    public ApiSuccessResponse<OrderDetailResponse> orderDetail(Authentication authentication,
                                                               @PathVariable UUID orderId,
                                                               HttpServletRequest request) {
        OrderDetailResponse data = OrderLifecycleApiMapper.toDetail(
                orderLifecycleService.merchantOrder(CurrentUser.userId(authentication), orderId)
        );
        return ApiSuccessResponse.of(data, RequestTrace.from(request));
    }

    @PatchMapping("/orders/{orderId}/status")
    public ApiSuccessResponse<OrderDetailResponse> updateOrderStatus(Authentication authentication,
                                                                     @PathVariable UUID orderId,
                                                                     @Valid @RequestBody OrderStatusUpdateApiRequest statusUpdate,
                                                                     HttpServletRequest request) {
        OrderStatus targetStatus = parseStatus(statusUpdate.status());
        OrderDetailResponse data = OrderLifecycleApiMapper.toDetail(
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
