package com.foodya.backend.application.service;

import com.foodya.backend.application.dto.CreateOrderItemRequest;
import com.foodya.backend.application.dto.CreateOrderRequest;
import com.foodya.backend.application.dto.OrderCreatedView;
import com.foodya.backend.application.event.OrderNotificationEvent;
import com.foodya.backend.application.exception.NotFoundException;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.port.out.MenuItemPort;
import com.foodya.backend.application.port.out.OrderItemPort;
import com.foodya.backend.application.port.out.OrderPaymentPort;
import com.foodya.backend.application.port.out.OrderPort;
import com.foodya.backend.application.port.out.RestaurantPort;
import com.foodya.backend.application.port.out.RouteDistancePort;
import com.foodya.backend.application.port.out.SystemParameterPort;
import com.foodya.backend.domain.model.OrderStatus;
import com.foodya.backend.domain.model.PaymentMethod;
import com.foodya.backend.domain.model.PaymentStatus;
import com.foodya.backend.domain.model.RestaurantStatus;
import com.foodya.backend.domain.persistence.MenuItem;
import com.foodya.backend.domain.persistence.Order;
import com.foodya.backend.domain.persistence.OrderItem;
import com.foodya.backend.domain.persistence.OrderPayment;
import com.foodya.backend.domain.persistence.Restaurant;
import com.foodya.backend.domain.persistence.SystemParameter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderCheckoutService {

    private final OrderPort orderPort;
    private final OrderItemPort orderItemPort;
    private final OrderPaymentPort orderPaymentPort;
    private final RestaurantPort restaurantPort;
    private final MenuItemPort menuItemPort;
    private final RouteDistancePort routeDistancePort;
    private final SystemParameterPort systemParameterPort;
    private final ApplicationEventPublisher applicationEventPublisher;

    public OrderCheckoutService(OrderPort orderPort,
                                OrderItemPort orderItemPort,
                                OrderPaymentPort orderPaymentPort,
                                RestaurantPort restaurantPort,
                                MenuItemPort menuItemPort,
                                RouteDistancePort routeDistancePort,
                                SystemParameterPort systemParameterPort,
                                ApplicationEventPublisher applicationEventPublisher) {
        this.orderPort = orderPort;
        this.orderItemPort = orderItemPort;
        this.orderPaymentPort = orderPaymentPort;
        this.restaurantPort = restaurantPort;
        this.menuItemPort = menuItemPort;
        this.routeDistancePort = routeDistancePort;
        this.systemParameterPort = systemParameterPort;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public OrderCreatedView createOrder(UUID customerUserId,
                                        String idempotencyKey,
                                        CreateOrderRequest request) {
        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);

        Order existing = orderPort.findByCustomerUserIdAndIdempotencyKey(customerUserId, normalizedIdempotencyKey).orElse(null);
        if (existing != null) {
            return toView(existing);
        }

        UUID restaurantId = parseUuid(request.restaurantId(), "restaurantId");
        Restaurant restaurant = restaurantPort.findByIdAndStatusIn(restaurantId, List.of(RestaurantStatus.ACTIVE))
                .orElseThrow(() -> new NotFoundException("restaurant not found"));

        if (request.items() == null || request.items().isEmpty()) {
            throw new ValidationException("order items are required", Map.of("items", "must not be empty"));
        }

        int currencyMinorUnit = intParam("currency.minor_unit", 0);
        RoundingMode roundingMode = roundingModeParam("currency.rounding_mode", "HALF_UP");

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CreateOrderItemRequest itemRequest : request.items()) {
            UUID menuItemId = parseUuid(itemRequest.menuItemId(), "menuItemId");
            MenuItem menuItem = menuItemPort.findById(menuItemId)
                    .orElseThrow(() -> new NotFoundException("menu item not found"));

            if (!restaurantId.equals(menuItem.getRestaurantId())) {
                throw new ValidationException("multi-restaurant order is not allowed", Map.of("items", "all items must belong to one restaurant"));
            }
            assertMenuItemOrderable(menuItem);

            BigDecimal lineTotal = round(menuItem.getPrice().multiply(BigDecimal.valueOf(itemRequest.quantity())), currencyMinorUnit, roundingMode);
            subtotal = subtotal.add(lineTotal);

            OrderItem line = new OrderItem();
            line.setMenuItemId(menuItem.getId());
            line.setMenuItemNameSnapshot(menuItem.getName());
            line.setUnitPriceSnapshot(menuItem.getPrice());
            line.setQuantity(itemRequest.quantity());
            line.setLineTotal(lineTotal);
            orderItems.add(line);
        }

        BigDecimal routeDistanceKm = routeDistancePort.routeDistanceKm(
                restaurant.getLatitude().doubleValue(),
                restaurant.getLongitude().doubleValue(),
                request.deliveryLatitude().doubleValue(),
                request.deliveryLongitude().doubleValue()
        );

        BigDecimal maxDeliveryKm = decimalParam("shipping.max_delivery_km", BigDecimal.valueOf(15));
        if (routeDistanceKm.compareTo(maxDeliveryKm) > 0) {
            throw new ValidationException("delivery distance exceeds platform limit",
                    Map.of("distanceKm", "must be <= shipping.max_delivery_km"));
        }

        BigDecimal deliveryFee = computeDeliveryFee(routeDistanceKm, currencyMinorUnit, roundingMode);
        BigDecimal total = round(subtotal.add(deliveryFee), currencyMinorUnit, roundingMode);

        BigDecimal commissionRatePercent = decimalParam("finance.commission_rate_percent", BigDecimal.TEN);
        BigDecimal shippingMarginRatePercent = decimalParam("finance.shipping_margin_rate_percent", BigDecimal.ZERO);

        BigDecimal commissionAmount = round(
                subtotal.multiply(commissionRatePercent).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP),
                currencyMinorUnit,
                roundingMode
        );
        BigDecimal shippingFeeMarginAmount = round(
                deliveryFee.multiply(shippingMarginRatePercent).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP),
                currencyMinorUnit,
                roundingMode
        );
        BigDecimal platformProfitAmount = round(commissionAmount.add(shippingFeeMarginAmount), currencyMinorUnit, roundingMode);

        Order order = new Order();
        order.setOrderCode(newOrderCode());
        order.setCustomerUserId(customerUserId);
        order.setIdempotencyKey(normalizedIdempotencyKey);
        order.setRestaurantId(restaurantId);
        order.setStatus(OrderStatus.PENDING);
        order.setDeliveryAddress(request.deliveryAddress().trim());
        order.setDeliveryLatitude(request.deliveryLatitude());
        order.setDeliveryLongitude(request.deliveryLongitude());
        order.setCustomerNote(request.customerNote());
        order.setSubtotalAmount(round(subtotal, currencyMinorUnit, roundingMode));
        order.setDeliveryFee(deliveryFee);
        order.setTotalAmount(total);
        order.setPaymentMethod(PaymentMethod.COD);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setCommissionAmount(commissionAmount);
        order.setShippingFeeMarginAmount(shippingFeeMarginAmount);
        order.setPlatformProfitAmount(platformProfitAmount);

        Order savedOrder = orderPort.save(order);

        for (OrderItem item : orderItems) {
            item.setOrderId(savedOrder.getId());
        }
        orderItemPort.saveAll(orderItems);

        OrderPayment payment = new OrderPayment();
        payment.setOrderId(savedOrder.getId());
        payment.setPaymentMethod(PaymentMethod.COD);
        payment.setPaymentStatus(PaymentStatus.UNPAID);
        payment.setAmount(savedOrder.getTotalAmount());
        orderPaymentPort.save(payment);

        applicationEventPublisher.publishEvent(new OrderNotificationEvent(
            savedOrder.getId(),
            savedOrder.getOrderCode(),
            savedOrder.getCustomerUserId(),
            restaurant.getOwnerUserId(),
            savedOrder.getStatus(),
            "ORDER_CREATED",
            "New order " + savedOrder.getOrderCode(),
            "Order placed and awaiting merchant acceptance"
        ));

        return toView(savedOrder);
    }

    private BigDecimal computeDeliveryFee(BigDecimal distanceKm,
                                          int minorUnit,
                                          RoundingMode roundingMode) {
        BigDecimal baseFee = decimalParam("shipping.base_delivery_fee", BigDecimal.valueOf(10000));
        BigDecimal baseDistanceKm = decimalParam("shipping.base_distance_km", BigDecimal.valueOf(2));
        BigDecimal feePerKm = decimalParam("shipping.fee_per_km", BigDecimal.valueOf(5000));

        if (distanceKm.compareTo(baseDistanceKm) <= 0) {
            return round(baseFee, minorUnit, roundingMode);
        }

        BigDecimal extraKm = distanceKm.subtract(baseDistanceKm);
        BigDecimal fee = baseFee.add(extraKm.multiply(feePerKm));
        return round(fee, minorUnit, roundingMode);
    }

    private static BigDecimal round(BigDecimal value, int minorUnit, RoundingMode roundingMode) {
        return value.setScale(minorUnit, roundingMode);
    }

    private BigDecimal decimalParam(String key, BigDecimal fallback) {
        return systemParameterPort.findById(key)
                .map(SystemParameter::getValue)
                .map(BigDecimal::new)
                .orElse(fallback);
    }

    private int intParam(String key, int fallback) {
        return systemParameterPort.findById(key)
                .map(SystemParameter::getValue)
                .map(Integer::parseInt)
                .orElse(fallback);
    }

    private RoundingMode roundingModeParam(String key, String fallback) {
        String value = systemParameterPort.findById(key)
                .map(SystemParameter::getValue)
                .orElse(fallback);
        return RoundingMode.valueOf(value);
    }

    private static String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ValidationException("missing Idempotency-Key", Map.of("Idempotency-Key", "header is required"));
        }
        String trimmed = idempotencyKey.trim();
        if (trimmed.length() > 80) {
            throw new ValidationException("invalid Idempotency-Key", Map.of("Idempotency-Key", "must be <= 80 chars"));
        }
        return trimmed;
    }

    private static UUID parseUuid(String raw, String field) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("invalid uuid", Map.of(field, "must be a valid UUID"));
        }
    }

    private static void assertMenuItemOrderable(MenuItem menuItem) {
        if (!menuItem.isActive() || !menuItem.isAvailable() || menuItem.getDeletedAt() != null) {
            throw new ValidationException("menu item is not orderable", Map.of("menuItemId", "inactive or unavailable"));
        }
    }

    private static String newOrderCode() {
        String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "ORD-" + timestamp + "-" + suffix;
    }

    private OrderCreatedView toView(Order order) {
        String currencyCode = systemParameterPort.findById("currency.code")
                .map(SystemParameter::getValue)
                .orElse("VND");
        return new OrderCreatedView(
                order.getId(),
                order.getOrderCode(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getSubtotalAmount(),
                order.getDeliveryFee(),
                order.getTotalAmount(),
                order.getCommissionAmount(),
                order.getShippingFeeMarginAmount(),
                order.getPlatformProfitAmount(),
                currencyCode
        );
    }
}
