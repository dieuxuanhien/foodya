package com.foodya.backend.application.service;

import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.exception.ConflictException;
import com.foodya.backend.application.exception.NotFoundException;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.port.out.AdminMenuItemPort;
import com.foodya.backend.application.port.out.AdminOrderPort;
import com.foodya.backend.application.port.out.AdminRestaurantPort;
import com.foodya.backend.domain.model.OrderStatus;
import com.foodya.backend.domain.model.RestaurantStatus;
import com.foodya.backend.domain.persistence.MenuItem;
import com.foodya.backend.domain.persistence.Order;
import com.foodya.backend.domain.persistence.Restaurant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminGovernanceService {

    private static final List<OrderStatus> BLOCKING_DELETE_STATUSES = List.of(
            OrderStatus.PENDING,
            OrderStatus.ACCEPTED,
            OrderStatus.ASSIGNED,
            OrderStatus.PREPARING,
            OrderStatus.DELIVERING
    );

    private final AdminRestaurantPort adminRestaurantPort;
    private final AdminOrderPort adminOrderPort;
    private final AdminMenuItemPort adminMenuItemPort;
    private final PaginationPolicyService paginationPolicyService;
    private final AuditLogService auditLogService;

    public AdminGovernanceService(AdminRestaurantPort adminRestaurantPort,
                                  AdminOrderPort adminOrderPort,
                                  AdminMenuItemPort adminMenuItemPort,
                                  PaginationPolicyService paginationPolicyService,
                                  AuditLogService auditLogService) {
        this.adminRestaurantPort = adminRestaurantPort;
        this.adminOrderPort = adminOrderPort;
        this.adminMenuItemPort = adminMenuItemPort;
        this.paginationPolicyService = paginationPolicyService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public PaginatedResult<Restaurant> listRestaurants(String keyword, RestaurantStatus status, Integer page, Integer size) {
        PaginationPolicyService.PaginationSpec spec = paginationPolicyService.page(page, size);
        return adminRestaurantPort.search(keyword, status, spec.page(), spec.size());
    }

    @Transactional
    public Restaurant approveRestaurant(UUID restaurantId, UUID actorId) {
        Restaurant restaurant = requireRestaurant(restaurantId);
        RestaurantStatus oldStatus = restaurant.getStatus();
        restaurant.setStatus(RestaurantStatus.ACTIVE);
        Restaurant saved = adminRestaurantPort.save(restaurant);
        auditLogService.securityEvent(
                actorId.toString(),
                "ADMIN_RESTAURANT_APPROVE",
                "RESTAURANT",
                restaurantId.toString(),
                oldStatus.name(),
                saved.getStatus().name()
        );
        return saved;
    }

    @Transactional
    public Restaurant rejectRestaurant(UUID restaurantId, UUID actorId) {
        Restaurant restaurant = requireRestaurant(restaurantId);
        RestaurantStatus oldStatus = restaurant.getStatus();
        restaurant.setStatus(RestaurantStatus.REJECTED);
        Restaurant saved = adminRestaurantPort.save(restaurant);
        auditLogService.securityEvent(
                actorId.toString(),
                "ADMIN_RESTAURANT_REJECT",
                "RESTAURANT",
                restaurantId.toString(),
                oldStatus.name(),
                saved.getStatus().name()
        );
        return saved;
    }

    @Transactional
    public void deleteRestaurant(UUID restaurantId, UUID actorId) {
        Restaurant restaurant = requireRestaurant(restaurantId);
        if (adminRestaurantPort.hasOrdersInStatuses(restaurantId, BLOCKING_DELETE_STATUSES)) {
            throw new ConflictException("hard delete blocked by linked active orders");
        }
        adminRestaurantPort.delete(restaurant);
        auditLogService.securityEvent(actorId.toString(), "ADMIN_RESTAURANT_DELETE", "RESTAURANT", restaurantId.toString(), null, "hard-deleted");
    }

    @Transactional(readOnly = true)
    public PaginatedResult<Order> listOrders(OrderStatus status, Integer page, Integer size) {
        PaginationPolicyService.PaginationSpec spec = paginationPolicyService.page(page, size);
        return adminOrderPort.search(status, spec.page(), spec.size());
    }

    @Transactional
    public Order updateOrderStatus(UUID orderId, OrderStatus targetStatus, UUID actorId) {
        Order order = requireOrder(orderId);
        OrderStatus current = order.getStatus();
        if (!isValidTransition(current, targetStatus)) {
            throw new ValidationException(
                    "invalid admin status transition",
                    Map.of("status", "must follow BR18 transition model")
            );
        }

        order.setStatus(targetStatus);
        Order saved = adminOrderPort.save(order);
        auditLogService.securityEvent(
                actorId.toString(),
                "ADMIN_ORDER_STATUS_UPDATE",
                "ORDER",
                orderId.toString(),
                current.name(),
                saved.getStatus().name()
        );
        return saved;
    }

    @Transactional
    public void deleteOrder(UUID orderId, UUID actorId) {
        Order order = requireOrder(orderId);
        adminOrderPort.delete(order);
        auditLogService.securityEvent(actorId.toString(), "ADMIN_ORDER_DELETE", "ORDER", orderId.toString(), null, "hard-deleted");
    }

    @Transactional
    public void hardDeleteMenuItem(UUID menuItemId, UUID actorId) {
        MenuItem menuItem = adminMenuItemPort.findById(menuItemId)
                .orElseThrow(() -> new NotFoundException("menu item not found"));
        adminMenuItemPort.delete(menuItem);
        auditLogService.securityEvent(actorId.toString(), "ADMIN_MENU_ITEM_HARD_DELETE", "MENU_ITEM", menuItemId.toString(), null, "hard-deleted");
    }

    private Restaurant requireRestaurant(UUID restaurantId) {
        return adminRestaurantPort.findById(restaurantId)
                .orElseThrow(() -> new NotFoundException("restaurant not found"));
    }

    private Order requireOrder(UUID orderId) {
        return adminOrderPort.findById(orderId)
                .orElseThrow(() -> new NotFoundException("order not found"));
    }

    private static boolean isValidTransition(OrderStatus current, OrderStatus target) {
        if (current == target) {
            return true;
        }

        return switch (current) {
            case PENDING -> target == OrderStatus.ACCEPTED || target == OrderStatus.CANCELLED || target == OrderStatus.FAILED;
            case ACCEPTED -> target == OrderStatus.ASSIGNED || target == OrderStatus.CANCELLED || target == OrderStatus.FAILED;
            case ASSIGNED -> target == OrderStatus.PREPARING
                    || target == OrderStatus.DELIVERING
                    || target == OrderStatus.CANCELLED
                    || target == OrderStatus.FAILED;
            case PREPARING -> target == OrderStatus.DELIVERING || target == OrderStatus.CANCELLED || target == OrderStatus.FAILED;
            case DELIVERING -> target == OrderStatus.SUCCESS || target == OrderStatus.FAILED;
            case SUCCESS, CANCELLED, FAILED -> false;
        };
    }
}
