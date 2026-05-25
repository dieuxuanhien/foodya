package com.foodya.backend.application.usecases;

import com.foodya.backend.application.dto.CreateMenuItemRequest;
import com.foodya.backend.application.dto.MenuCategoryData;
import com.foodya.backend.application.dto.MenuItemData;
import com.foodya.backend.application.dto.OrderData;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.dto.RestaurantData;
import com.foodya.backend.application.dto.UpdateMenuItemRequest;
import com.foodya.backend.application.exception.ConflictException;
import com.foodya.backend.application.exception.NotFoundException;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.ports.in.AdminGovernanceUseCase;
import com.foodya.backend.application.ports.out.AdminMenuItemPort;
import com.foodya.backend.application.ports.out.AdminOrderPort;
import com.foodya.backend.application.ports.out.AdminRestaurantPort;
import com.foodya.backend.application.ports.out.AdminUserPort;
import com.foodya.backend.application.ports.out.CategoryTaxonomyPort;
import com.foodya.backend.application.ports.out.MenuCategoryPort;
import com.foodya.backend.application.ports.out.OrderPaymentPort;
import com.foodya.backend.application.ports.out.SystemParameterPort;
import com.foodya.backend.application.support.PaginationPolicy;
import com.foodya.backend.domain.entities.MenuCategory;
import com.foodya.backend.domain.entities.MenuItem;
import com.foodya.backend.domain.entities.Order;
import com.foodya.backend.domain.entities.OrderPayment;
import com.foodya.backend.domain.entities.Restaurant;
import com.foodya.backend.domain.entities.SystemParameter;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.value_objects.PaymentStatus;
import com.foodya.backend.domain.value_objects.RestaurantStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AdminGovernanceService implements AdminGovernanceUseCase {

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
    private final MenuCategoryPort menuCategoryPort;
    private final CategoryTaxonomyPort categoryTaxonomyPort;
    private final SystemParameterPort systemParameterPort;
    private final AdminUserPort adminUserPort;
    private final PaginationPolicy paginationPolicy;
    private final AuditLogService auditLogService;
    private final OrderPaymentPort orderPaymentPort;

    public AdminGovernanceService(AdminRestaurantPort adminRestaurantPort,
                                  AdminOrderPort adminOrderPort,
                                  AdminMenuItemPort adminMenuItemPort,
                                  MenuCategoryPort menuCategoryPort,
                                  CategoryTaxonomyPort categoryTaxonomyPort,
                                  SystemParameterPort systemParameterPort,
                                  AdminUserPort adminUserPort,
                                  PaginationPolicy paginationPolicy,
                                  AuditLogService auditLogService,
                                  OrderPaymentPort orderPaymentPort) {
        this.adminRestaurantPort = adminRestaurantPort;
        this.adminOrderPort = adminOrderPort;
        this.adminMenuItemPort = adminMenuItemPort;
        this.menuCategoryPort = menuCategoryPort;
        this.categoryTaxonomyPort = categoryTaxonomyPort;
        this.systemParameterPort = systemParameterPort;
        this.adminUserPort = adminUserPort;
        this.paginationPolicy = paginationPolicy;
        this.auditLogService = auditLogService;
        this.orderPaymentPort = orderPaymentPort;
    }

    public PaginatedResult<RestaurantData> listRestaurants(String keyword, RestaurantStatus status, Integer page, Integer size) {
        PaginationPolicy.PaginationSpec spec = paginationPolicy.page(page, size);
        PaginatedResult<Restaurant> result = adminRestaurantPort.search(keyword, status, spec.page(), spec.size());
        return new PaginatedResult<>(
                result.items().stream().map(this::toRestaurantData).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    public RestaurantData getRestaurantById(UUID restaurantId) {
        Restaurant restaurant = requireRestaurant(restaurantId);
        return toRestaurantData(restaurant);
    }

    public RestaurantData createRestaurant(com.foodya.backend.application.dto.AdminRestaurantCreateCommand command, UUID actorId) {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(UUID.randomUUID());
        restaurant.setOwnerUserId(command.ownerUserId());
        restaurant.setName(command.name());
        restaurant.setDescription(command.description());
        restaurant.setAddressLine(command.addressLine());
        restaurant.setCuisineType(command.cuisineType());
        restaurant.setOpen(command.isOpen());
        restaurant.setStatus(command.status());
        restaurant.setLatitude(command.latitude());
        restaurant.setLongitude(command.longitude());
        restaurant.setMaxDeliveryKm(command.maxDeliveryKm());
        restaurant.setCreatedAt(java.time.OffsetDateTime.now());
        restaurant.setUpdatedAt(restaurant.getCreatedAt());

        Restaurant saved = adminRestaurantPort.save(restaurant);
        auditLogService.securityEvent(actorId.toString(), "ADMIN_RESTAURANT_CREATE", "RESTAURANT", saved.getId().toString(), null, saved.getStatus().name());
        return toRestaurantData(saved);
    }

    public RestaurantData updateRestaurant(UUID restaurantId, com.foodya.backend.application.dto.AdminRestaurantUpdateCommand command, UUID actorId) {
        Restaurant restaurant = requireRestaurant(restaurantId);
        
        restaurant.setOwnerUserId(command.ownerUserId());
        restaurant.setName(command.name());
        restaurant.setDescription(command.description());
        restaurant.setAddressLine(command.addressLine());
        restaurant.setCuisineType(command.cuisineType());
        restaurant.setOpen(command.isOpen());
        restaurant.setStatus(command.status());
        restaurant.setLatitude(command.latitude());
        restaurant.setLongitude(command.longitude());
        restaurant.setMaxDeliveryKm(command.maxDeliveryKm());
        restaurant.setUpdatedAt(java.time.OffsetDateTime.now());

        Restaurant saved = adminRestaurantPort.save(restaurant);
        auditLogService.securityEvent(actorId.toString(), "ADMIN_RESTAURANT_UPDATE", "RESTAURANT", saved.getId().toString(), null, "Updated");
        return toRestaurantData(saved);
    }

    public RestaurantData approveRestaurant(UUID restaurantId, UUID actorId) {
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
        return toRestaurantData(saved);
    }

    public RestaurantData rejectRestaurant(UUID restaurantId, UUID actorId) {
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
        return toRestaurantData(saved);
    }

    public void deleteRestaurant(UUID restaurantId, UUID actorId) {
        Restaurant restaurant = requireRestaurant(restaurantId);
        if (adminRestaurantPort.hasOrdersInStatuses(restaurantId, BLOCKING_DELETE_STATUSES)) {
            throw new ConflictException("hard delete blocked by linked active orders");
        }
        adminRestaurantPort.delete(restaurant);
        auditLogService.securityEvent(actorId.toString(), "ADMIN_RESTAURANT_DELETE", "RESTAURANT", restaurantId.toString(), null, "hard-deleted");
    }

    public PaginatedResult<OrderData> listOrders(OrderStatus status, Integer page, Integer size) {
        PaginationPolicy.PaginationSpec spec = paginationPolicy.page(page, size);
        PaginatedResult<Order> result = adminOrderPort.search(status, spec.page(), spec.size());
        return new PaginatedResult<>(
                result.items().stream().map(this::toOrderData).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    public PaginatedResult<MenuCategoryData> listMenuCategories(UUID restaurantId, Integer page, Integer size) {
        requireRestaurant(restaurantId);
        PaginationPolicy.PaginationSpec spec = paginationPolicy.page(page, size);
        PaginatedResult<MenuCategory> result = menuCategoryPort.findByRestaurantIdAndActiveTrue(restaurantId, spec.page(), spec.size());
        return new PaginatedResult<>(
                result.items().stream().map(this::toMenuCategoryData).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    public PaginatedResult<MenuItemData> listMenuItems(UUID restaurantId, String taxonomyCode, String keyword, Integer page, Integer size) {
        PaginationPolicy.PaginationSpec spec = paginationPolicy.page(page, size);
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        String normalizedTaxonomy = taxonomyCode == null || taxonomyCode.isBlank() ? null : taxonomyCode.trim();
        PaginatedResult<MenuItem> result = adminMenuItemPort.search(
                restaurantId,
                normalizedTaxonomy,
                normalizedKeyword,
                spec.page(),
                spec.size()
        );

        return new PaginatedResult<>(
                result.items().stream().map(this::toMenuItemData).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    public MenuItemData createMenuItem(UUID restaurantId, CreateMenuItemRequest request, UUID actorId) {
        requireRestaurant(restaurantId);
        requireText(request.categoryId(), "categoryId");
        requireNonEmptyTaxonomyCodes(request.taxonomyCodes(), "taxonomyCodes");
        requireText(request.name(), "name");
        requireText(request.description(), "description");
        requireNonNull(request.price(), "price");
        requireNonNull(request.isActive(), "isActive");
        requireNonNull(request.isAvailable(), "isAvailable");
        validatePrice(request.price());

        UUID categoryId = parseUuid(request.categoryId(), "categoryId");
        menuCategoryPort.findByIdAndRestaurantId(categoryId, restaurantId)
                .orElseThrow(() -> new ValidationException("invalid category", Map.of("categoryId", "does not belong to restaurant")));

        List<String> taxonomyCodes = normalizeTaxonomyCodes(request.taxonomyCodes());
        validateTaxonomyCodes(taxonomyCodes);

        MenuItem item = new MenuItem();
        item.setId(UUID.randomUUID());
        item.setRestaurantId(restaurantId);
        item.setCategoryId(categoryId);
        item.setTaxonomyCodes(new LinkedHashSet<>(taxonomyCodes));
        item.setName(request.name().trim());
        item.setDescription(request.description());
        item.setPrice(request.price());
        item.setActive(request.isActive());
        item.setAvailable(request.isAvailable());
        item.setCreatedAt(OffsetDateTime.now());
        item.setUpdatedAt(item.getCreatedAt());

        MenuItem saved = adminMenuItemPort.save(item);
        auditLogService.securityEvent(actorId.toString(), "ADMIN_MENU_ITEM_CREATE", "MENU_ITEM", saved.getId().toString(), null, "created");
        return toMenuItemData(saved);
    }

    public MenuItemData updateMenuItem(UUID menuItemId, UpdateMenuItemRequest request, UUID actorId) {
        requireText(request.categoryId(), "categoryId");
        requireNonEmptyTaxonomyCodes(request.taxonomyCodes(), "taxonomyCodes");
        requireText(request.name(), "name");
        requireText(request.description(), "description");
        requireNonNull(request.price(), "price");
        requireNonNull(request.isActive(), "isActive");
        requireNonNull(request.isAvailable(), "isAvailable");
        validatePrice(request.price());

        MenuItem item = adminMenuItemPort.findById(menuItemId)
                .orElseThrow(() -> new NotFoundException("menu item not found"));

        UUID categoryId = parseUuid(request.categoryId(), "categoryId");
        menuCategoryPort.findByIdAndRestaurantId(categoryId, item.getRestaurantId())
                .orElseThrow(() -> new ValidationException("invalid category", Map.of("categoryId", "does not belong to restaurant")));

        List<String> taxonomyCodes = normalizeTaxonomyCodes(request.taxonomyCodes());
        validateTaxonomyCodes(taxonomyCodes);

        item.setCategoryId(categoryId);
        item.setTaxonomyCodes(new LinkedHashSet<>(taxonomyCodes));
        item.setName(request.name().trim());
        item.setDescription(request.description());
        item.setPrice(request.price());
        item.setActive(request.isActive());
        item.setAvailable(request.isAvailable());
        item.setUpdatedAt(OffsetDateTime.now());

        MenuItem saved = adminMenuItemPort.save(item);
        auditLogService.securityEvent(actorId.toString(), "ADMIN_MENU_ITEM_UPDATE", "MENU_ITEM", saved.getId().toString(), null, "updated");
        return toMenuItemData(saved);
    }

    public OrderData updateOrderStatus(UUID orderId, OrderStatus targetStatus, UUID actorId) {
        Order order = requireOrder(orderId);
        OrderStatus current = order.getStatus();
        if (!isValidTransition(current, targetStatus)) {
            throw new ValidationException(
                    "invalid admin status transition",
                    Map.of("status", "must follow BR18 transition model")
            );
        }

        order.adminTransitionTo(targetStatus);
        syncCodPaymentState(order, targetStatus);
        Order saved = adminOrderPort.save(order);
        auditLogService.securityEvent(
                actorId.toString(),
                "ADMIN_ORDER_STATUS_UPDATE",
                "ORDER",
                orderId.toString(),
                current.name(),
                saved.getStatus().name()
        );
        return toOrderData(saved);
    }

    public void deleteOrder(UUID orderId, UUID actorId) {
        Order order = requireOrder(orderId);
        adminOrderPort.delete(order);
        auditLogService.securityEvent(actorId.toString(), "ADMIN_ORDER_DELETE", "ORDER", orderId.toString(), null, "hard-deleted");
    }

    public void hardDeleteMenuItem(UUID menuItemId, UUID actorId) {
        MenuItem menuItem = adminMenuItemPort.findById(menuItemId)
                .orElseThrow(() -> new NotFoundException("menu item not found"));
        adminMenuItemPort.delete(menuItem);
        auditLogService.securityEvent(actorId.toString(), "ADMIN_MENU_ITEM_HARD_DELETE", "MENU_ITEM", menuItemId.toString(), null, "hard-deleted");
    }

    private void validatePrice(BigDecimal price) {
        BigDecimal minPrice = systemParameterPort.findById("catalog.menu_item_price_min")
                .map(SystemParameter::getValue)
                .map(BigDecimal::new)
                .orElse(BigDecimal.ONE);
        BigDecimal maxPrice = systemParameterPort.findById("catalog.menu_item_price_max")
                .map(SystemParameter::getValue)
                .map(BigDecimal::new)
                .orElse(new BigDecimal("10000000"));

        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("invalid menu item price", Map.of("price", "must be > 0"));
        }
        if (price.compareTo(minPrice) < 0 || price.compareTo(maxPrice) > 0) {
            throw new ValidationException(
                    "invalid menu item price",
                    Map.of("price", "must be between " + minPrice.toPlainString() + " and " + maxPrice.toPlainString())
            );
        }
    }

    private void validateTaxonomyCodes(List<String> taxonomyCodes) {
        for (String taxonomyCode : taxonomyCodes) {
            categoryTaxonomyPort.findByCode(taxonomyCode)
                    .orElseThrow(() -> new ValidationException("invalid taxonomyCode", Map.of("taxonomyCode", taxonomyCode + " does not exist or is inactive")));
        }
    }

    private static List<String> normalizeTaxonomyCodes(List<String> taxonomyCodes) {
        if (taxonomyCodes == null || taxonomyCodes.isEmpty()) {
            return List.of();
        }
        return taxonomyCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private static void requireNonEmptyTaxonomyCodes(List<String> taxonomyCodes, String field) {
        if (taxonomyCodes == null || taxonomyCodes.isEmpty()) {
            throw new ValidationException("invalid " + field, Map.of(field, "must not be empty"));
        }
    }

    private static UUID parseUuid(String value, String field) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("invalid uuid", Map.of(field, "must be a valid UUID"));
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("invalid " + field, Map.of(field, "must not be blank"));
        }
    }

    private static void requireNonNull(Object value, String field) {
        if (value == null) {
            throw new ValidationException("invalid " + field, Map.of(field, "must not be null"));
        }
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
                    || target == OrderStatus.CANCELLED
                    || target == OrderStatus.FAILED;
            case PREPARING -> target == OrderStatus.DELIVERING || target == OrderStatus.CANCELLED || target == OrderStatus.FAILED;
            case DELIVERING -> target == OrderStatus.SUCCESS || target == OrderStatus.FAILED;
            case SUCCESS, CANCELLED, FAILED -> false;
        };
    }

    private void syncCodPaymentState(Order order, OrderStatus targetStatus) {
        if (targetStatus == OrderStatus.SUCCESS) {
            order.markCodPaid();
        } else if (targetStatus == OrderStatus.FAILED) {
            order.markCodFailed();
        } else {
            return;
        }

        OrderPayment payment = orderPaymentPort.findByOrderId(order.getId())
                .orElseGet(OrderPayment::new);
        payment.setOrderId(order.getId());
        payment.setPaymentMethod(order.getPaymentMethod());
        payment.setPaymentStatus(order.getPaymentStatus());
        payment.setAmount(order.getTotalAmount());
        payment.setPaidAt(order.getPaymentStatus() == PaymentStatus.PAID
                ? (order.getCompletedAt() == null ? OffsetDateTime.now() : order.getCompletedAt())
                : null);
        orderPaymentPort.save(payment);
    }

    private RestaurantData toRestaurantData(Restaurant restaurant) {
        RestaurantData model = new RestaurantData();
        model.setId(restaurant.getId());
        model.setOwnerUserId(restaurant.getOwnerUserId());
        model.setName(restaurant.getName());
        model.setCuisineType(restaurant.getCuisineType());
        model.setCuisineTypes(restaurant.getCuisineTypes());
        model.setDescription(restaurant.getDescription());
        model.setBackgroundImageUrl(restaurant.getBackgroundImageUrl());
        model.setAvatarImageUrl(restaurant.getAvatarImageUrl());
        model.setAddressLine(restaurant.getAddressLine());
        model.setLatitude(restaurant.getLatitude());
        model.setLongitude(restaurant.getLongitude());
        model.setH3IndexRes9(restaurant.getH3IndexRes9());
        model.setAvgRating(restaurant.getAvgRating());
        model.setReviewCount(restaurant.getReviewCount());
        model.setStatus(restaurant.getStatus());
        model.setOpen(restaurant.isOpen());
        model.setMaxDeliveryKm(restaurant.getMaxDeliveryKm());
        return model;
    }

    private MenuCategoryData toMenuCategoryData(MenuCategory category) {
        MenuCategoryData model = new MenuCategoryData();
        model.setId(category.getId());
        model.setRestaurantId(category.getRestaurantId());
        model.setName(category.getName());
        model.setSortOrder(category.getSortOrder());
        model.setActive(category.isActive());
        return model;
    }

    private OrderData toOrderData(Order order) {
        OrderData model = new OrderData();
        model.setId(order.getId());
        model.setOrderCode(order.getOrderCode());
        model.setCustomerUserId(order.getCustomerUserId());
        model.setIdempotencyKey(order.getIdempotencyKey());
        model.setRestaurantId(order.getRestaurantId());
        model.setStatus(order.getStatus());
        model.setDeliveryAddress(order.getDeliveryAddress());
        model.setDeliveryLatitude(order.getDeliveryLatitude());
        model.setDeliveryLongitude(order.getDeliveryLongitude());
        model.setCustomerNote(order.getCustomerNote());
        model.setSubtotalAmount(order.getSubtotalAmount());
        model.setDeliveryFee(order.getDeliveryFee());
        model.setTotalAmount(order.getTotalAmount());
        model.setPaymentMethod(order.getPaymentMethod());
        model.setPaymentStatus(order.getPaymentStatus());
        model.setCommissionAmount(order.getCommissionAmount());
        model.setShippingFeeMarginAmount(order.getShippingFeeMarginAmount());
        model.setPlatformProfitAmount(order.getPlatformProfitAmount());

        adminRestaurantPort.findById(order.getRestaurantId())
                .ifPresent(r -> model.setRestaurantName(r.getName()));
        adminUserPort.findById(order.getCustomerUserId())
                .ifPresent(u -> model.setCustomerName(u.getFullName()));

        return model;
    }

    private MenuItemData toMenuItemData(MenuItem menuItem) {
        MenuItemData model = new MenuItemData();
        model.setId(menuItem.getId());
        model.setRestaurantId(menuItem.getRestaurantId());
        model.setCategoryId(menuItem.getCategoryId());
        model.setTaxonomyCodes(menuItem.getTaxonomyCodes().stream().toList());
        model.setName(menuItem.getName());
        model.setDescription(menuItem.getDescription());
        model.setImageUrl(menuItem.getImageUrl());
        model.setPrice(menuItem.getPrice());
        model.setActive(menuItem.isActive());
        model.setAvailable(menuItem.isAvailable());
        model.setDeletedAt(menuItem.getDeletedAt());

        adminRestaurantPort.findById(menuItem.getRestaurantId())
                .ifPresent(r -> model.setRestaurantName(r.getName()));
        if (menuItem.getCategoryId() != null) {
            menuCategoryPort.findById(menuItem.getCategoryId())
                    .ifPresent(c -> model.setCategoryName(c.getName()));
        }

        return model;
    }
}
