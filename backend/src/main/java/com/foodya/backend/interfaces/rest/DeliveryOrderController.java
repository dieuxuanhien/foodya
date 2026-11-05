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
import com.foodya.backend.interfaces.rest.support.CurrentUser;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
@Tag(name = "Driver Orders", description = "Driver order assignments, acceptance, status transitions and live tracking upload")
public class DeliveryOrderController {

    private final OrderLifecycleUseCase orderLifecycleService;

    public DeliveryOrderController(OrderLifecycleUseCase orderLifecycleService) {
        this.orderLifecycleService = orderLifecycleService;
    }

    @Operation(summary = "List assigned orders",
            description = "Orders the calling driver currently holds: ASSIGNED, PREPARING or DELIVERING. Orders held by other drivers, and orders not yet assigned, are never returned.")
    @GetMapping("/assignments")
    public ResponseEntity<ApiSuccessResponse<List<OrderSummaryResponse>>> assignments(Authentication authentication,
                                                                                   HttpServletRequest request) {
        List<OrderSummaryResponse> data = orderLifecycleService.deliveryAssignments(CurrentUser.userId(authentication)).stream()
                .map(OrderLifecycleApiMapper::toSummary)
                .toList();
        return ResponseEntity.ok(ApiSuccessResponse.of(data, RequestTrace.from(request)));
    }

    @Operation(summary = "Confirm pickup of an assigned order",
            description = "Only the assigned driver may call this. The order already counts as ASSIGNED; this is an acknowledgement, so the status does not change. Advance the order with PATCH /status.")
    @PostMapping("/{orderId}/accept")
    public ResponseEntity<ApiSuccessResponse<OrderDetailResponse>> accept(@PathVariable UUID orderId,
                                                          Authentication authentication,
                                                          HttpServletRequest request) {
        OrderDetailResponse data = OrderLifecycleApiMapper.toDetail(
                orderLifecycleService.deliveryAccept(CurrentUser.userId(authentication), orderId));
        return ResponseEntity.ok(ApiSuccessResponse.of(data, RequestTrace.from(request)));
    }

    @Operation(summary = "Update delivery status",
            description = "Only the assigned driver may call this. DELIVERING = picked up from the restaurant (allowed from ASSIGNED or PREPARING). SUCCESS = delivered and cash collected on COD. FAILED = could not deliver.")
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<ApiSuccessResponse<OrderDetailResponse>> updateStatus(@PathVariable UUID orderId,
                                                                 Authentication authentication,
                                                                 @Valid @RequestBody OrderStatusUpdateApiRequest statusUpdate,
                                                                 HttpServletRequest request) {
        OrderStatus targetStatus = parseStatus(statusUpdate.status());
        OrderDetailResponse data = OrderLifecycleApiMapper.toDetail(
                orderLifecycleService.deliveryUpdateStatus(CurrentUser.userId(authentication), orderId, targetStatus));
        return ResponseEntity.ok(ApiSuccessResponse.of(data, RequestTrace.from(request)));
    }

    @Operation(summary = "Decline an assigned order",
            description = "Only the assigned driver may call this, and only before pickup. The order goes back to ACCEPTED for reassignment. Once the driver has started delivery, support must get involved.")
    @PostMapping("/{orderId}/reject")
    public ResponseEntity<ApiSuccessResponse<OrderDetailResponse>> reject(@PathVariable UUID orderId,
                                                          Authentication authentication,
                                                          HttpServletRequest request) {
        OrderDetailResponse data = OrderLifecycleApiMapper.toDetail(
                orderLifecycleService.deliveryReject(CurrentUser.userId(authentication), orderId));
        return ResponseEntity.ok(ApiSuccessResponse.of(data, RequestTrace.from(request)));
    }

    @Operation(summary = "Send a GPS point",
            description = "Records the driver's GPS point, snaps it to the planned route, and pushes a "
                    + "tracking update over STOMP to /user/queue/orders/{orderId}/tracking.")
    @PostMapping("/{orderId}/tracking-points")
    public ResponseEntity<ApiSuccessResponse<OrderTrackingPointResponse>> addTrackingPoint(@PathVariable UUID orderId,
                                                                            Authentication authentication,
                                                                            @Valid @RequestBody DeliveryLocationUpdateApiRequest updateRequest,
                                                                            HttpServletRequest request) {
        OrderTrackingPointResponse data = OrderLifecycleApiMapper.toTrackingPoint(
                orderLifecycleService.addTrackingPoint(CurrentUser.userId(authentication), orderId,
                        updateRequest.lat(), updateRequest.lng(), updateRequest.recordedAt())
        );
        return ResponseEntity.status(HttpStatus.CREATED)
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
