package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.dto.OrderModel;
import com.foodya.backend.application.dto.RestaurantModel;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.ports.in.AdminGovernanceUseCase;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.value_objects.RestaurantStatus;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.OrderDetailResponse;
import com.foodya.backend.interfaces.rest.dto.OrderStatusUpdateRestRequest;
import com.foodya.backend.interfaces.rest.dto.OrderSummaryResponse;
import com.foodya.backend.interfaces.rest.dto.PageMeta;
import com.foodya.backend.interfaces.rest.dto.RestaurantDetailResponse;
import com.foodya.backend.interfaces.rest.mapper.RestDtoMapper;
import com.foodya.backend.interfaces.rest.support.CurrentUser;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminGovernanceController {

    private final AdminGovernanceUseCase adminGovernanceService;

    public AdminGovernanceController(AdminGovernanceUseCase adminGovernanceService) {
        this.adminGovernanceService = adminGovernanceService;
    }

    @GetMapping("/restaurants")
    public ResponseEntity<ApiSuccessResponse<List<RestaurantDetailResponse>>> listRestaurants(@RequestParam(required = false) String q,
                                                                                                @RequestParam(required = false) String status,
                                                                                                @RequestParam(required = false) Integer page,
                                                                                                @RequestParam(required = false) Integer size,
                                                                                                HttpServletRequest request) {
        RestaurantStatus restaurantStatus = parseRestaurantStatus(status);
        PaginatedResult<RestaurantModel> result = adminGovernanceService.listRestaurants(q, restaurantStatus, page, size);

        List<RestaurantDetailResponse> data = result.items().stream()
                .map(RestDtoMapper::toRestaurantDetailResponse)
                .toList();

        return ResponseEntity.ok(ApiSuccessResponse.of(
                data,
                new PageMeta(result.page(), result.size(), result.totalElements(), result.totalPages()),
                RequestTrace.from(request)
        ));
    }

    @PostMapping("/restaurants/{id}/approve")
    public ResponseEntity<ApiSuccessResponse<RestaurantDetailResponse>> approveRestaurant(Authentication authentication,
                                                                                           @PathVariable String id,
                                                                                           HttpServletRequest request) {
        RestaurantModel restaurant = adminGovernanceService.approveRestaurant(parseUuid(id, "id"), CurrentUser.userId(authentication));
        return ResponseEntity.ok(ApiSuccessResponse.of(RestDtoMapper.toRestaurantDetailResponse(restaurant), RequestTrace.from(request)));
    }

    @PostMapping("/restaurants/{id}/reject")
    public ResponseEntity<ApiSuccessResponse<RestaurantDetailResponse>> rejectRestaurant(Authentication authentication,
                                                                                          @PathVariable String id,
                                                                                          HttpServletRequest request) {
        RestaurantModel restaurant = adminGovernanceService.rejectRestaurant(parseUuid(id, "id"), CurrentUser.userId(authentication));
        return ResponseEntity.ok(ApiSuccessResponse.of(RestDtoMapper.toRestaurantDetailResponse(restaurant), RequestTrace.from(request)));
    }

    @DeleteMapping("/restaurants/{id}")
    public ResponseEntity<Void> deleteRestaurant(Authentication authentication, @PathVariable String id) {
        adminGovernanceService.deleteRestaurant(parseUuid(id, "id"), CurrentUser.userId(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiSuccessResponse<List<OrderSummaryResponse>>> listOrders(@RequestParam(required = false) String status,
                                                                                      @RequestParam(required = false) Integer page,
                                                                                      @RequestParam(required = false) Integer size,
                                                                                      HttpServletRequest request) {
        OrderStatus orderStatus = parseOrderStatus(status);
        PaginatedResult<OrderModel> result = adminGovernanceService.listOrders(orderStatus, page, size);

        List<OrderSummaryResponse> data = result.items().stream()
                .map(this::toOrderSummary)
                .toList();

        return ResponseEntity.ok(ApiSuccessResponse.of(
                data,
                new PageMeta(result.page(), result.size(), result.totalElements(), result.totalPages()),
                RequestTrace.from(request)
        ));
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<ApiSuccessResponse<OrderDetailResponse>> updateOrderStatus(Authentication authentication,
                                                                                      @PathVariable String id,
                                                                                      @Valid @RequestBody OrderStatusUpdateRestRequest request,
                                                                                      HttpServletRequest httpServletRequest) {
        OrderModel order = adminGovernanceService.updateOrderStatus(
                parseUuid(id, "id"),
                parseOrderStatus(request.status()),
                CurrentUser.userId(authentication)
        );
        return ResponseEntity.ok(ApiSuccessResponse.of(toOrderDetail(order), RequestTrace.from(httpServletRequest)));
    }

    @DeleteMapping("/orders/{id}")
    public ResponseEntity<Void> deleteOrder(Authentication authentication, @PathVariable String id) {
        adminGovernanceService.deleteOrder(parseUuid(id, "id"), CurrentUser.userId(authentication));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/menu-items/{id}")
    public ResponseEntity<Void> hardDeleteMenuItem(Authentication authentication, @PathVariable String id) {
        adminGovernanceService.hardDeleteMenuItem(parseUuid(id, "id"), CurrentUser.userId(authentication));
        return ResponseEntity.noContent().build();
    }

    private static UUID parseUuid(String raw, String field) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("invalid uuid", Map.of(field, "must be UUID"));
        }
    }

    private static RestaurantStatus parseRestaurantStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return RestaurantStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("invalid restaurant status", Map.of("status", "unknown status value"));
        }
    }

    private static OrderStatus parseOrderStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("invalid order status", Map.of("status", "unknown status value"));
        }
    }

    private OrderSummaryResponse toOrderSummary(OrderModel order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderCode(),
                order.getStatus().name(),
                order.getPaymentStatus().name(),
                order.getTotalAmount()
        );
    }

    private OrderDetailResponse toOrderDetail(OrderModel order) {
        return new OrderDetailResponse(
                order.getId(),
                order.getOrderCode(),
                order.getRestaurantId(),
                order.getCustomerUserId(),
                order.getStatus().name(),
                order.getPaymentMethod().name(),
                order.getPaymentStatus().name(),
                order.getSubtotalAmount(),
                order.getDeliveryFee(),
                order.getTotalAmount(),
                order.getDeliveryAddress()
        );
    }
}
