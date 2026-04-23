package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.ports.in.OrderLifecycleUseCase;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.DeliveryLocationUpdateApiRequest;
import com.foodya.backend.interfaces.rest.dto.OrderDetailResponse;
import com.foodya.backend.interfaces.rest.dto.OrderStatusUpdateApiRequest;
import com.foodya.backend.interfaces.rest.dto.OrderSummaryResponse;
import com.foodya.backend.interfaces.rest.dto.OrderTrackingPointResponse;
import com.foodya.backend.interfaces.rest.mapper.OrderLifecycleApiMapper;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/delivery/orders")
public class DeliveryOrderController {

    private final OrderLifecycleUseCase orderLifecycleService;

    public DeliveryOrderController(OrderLifecycleUseCase orderLifecycleService) {
        this.orderLifecycleService = orderLifecycleService;
    }

    @GetMapping({"/assignments", "/assigned"})
    public ApiSuccessResponse<List<OrderSummaryResponse>> assignments(HttpServletRequest request) {
        List<OrderSummaryResponse> data = orderLifecycleService.deliveryAssignments().stream()
                .map(OrderLifecycleApiMapper::toSummary)
                .toList();
        return ApiSuccessResponse.of(data, RequestTrace.from(request));
    }

    @PostMapping("/{orderId}/accept")
    public ApiSuccessResponse<OrderDetailResponse> accept(@PathVariable UUID orderId,
                                                          HttpServletRequest request) {
        OrderDetailResponse data = OrderLifecycleApiMapper.toDetail(orderLifecycleService.deliveryAccept(orderId));
        return ApiSuccessResponse.of(data, RequestTrace.from(request));
    }

    @PatchMapping("/{orderId}/status")
    public ApiSuccessResponse<OrderDetailResponse> updateStatus(@PathVariable UUID orderId,
                                                                 @Valid @RequestBody OrderStatusUpdateApiRequest statusUpdate,
                                                                 HttpServletRequest request) {
        OrderStatus targetStatus = parseStatus(statusUpdate.status());
        OrderDetailResponse data = OrderLifecycleApiMapper.toDetail(orderLifecycleService.deliveryUpdateStatus(orderId, targetStatus));
        return ApiSuccessResponse.of(data, RequestTrace.from(request));
    }

    @PostMapping("/{orderId}/tracking-points")
    public ApiSuccessResponse<OrderTrackingPointResponse> addTrackingPoint(@PathVariable UUID orderId,
                                                                            @Valid @RequestBody DeliveryLocationUpdateApiRequest updateRequest,
                                                                            HttpServletRequest request) {
        OrderTrackingPointResponse data = OrderLifecycleApiMapper.toTrackingPoint(
                orderLifecycleService.addTrackingPoint(orderId, updateRequest.lat(), updateRequest.lng(), updateRequest.recordedAt())
        );
        return ApiSuccessResponse.of(data, RequestTrace.from(request));
    }

    @PostMapping("/{orderId}/locations")
    public ResponseEntity<ApiSuccessResponse<OrderTrackingPointResponse>> addLocationPoint(@PathVariable UUID orderId,
                                                                                             @Valid @RequestBody DeliveryLocationUpdateApiRequest updateRequest,
                                                                                             HttpServletRequest request) {
        OrderTrackingPointResponse data = OrderLifecycleApiMapper.toTrackingPoint(
                orderLifecycleService.addTrackingPoint(orderId, updateRequest.lat(), updateRequest.lng(), updateRequest.recordedAt())
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiSuccessResponse.of(data, RequestTrace.from(request)));
    }

    private static OrderStatus parseStatus(String rawStatus) {
        try {
            return OrderStatus.valueOf(rawStatus.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("invalid order status", Map.of("status", "unknown status value"));
        }
    }
}
