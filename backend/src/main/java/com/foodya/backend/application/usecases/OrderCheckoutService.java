package com.foodya.backend.application.usecases;

import com.foodya.backend.application.dto.CreateOrderItemRequest;
import com.foodya.backend.application.dto.CreateOrderRequest;
import com.foodya.backend.application.dto.OrderCreatedView;
import com.foodya.backend.application.event.OrderNotificationEvent;
import com.foodya.backend.application.exception.NotFoundException;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.ports.in.OrderCheckoutUseCase;
import com.foodya.backend.application.dto.MenuItemModel;
import com.foodya.backend.application.dto.OrderItemModel;
import com.foodya.backend.application.dto.OrderModel;
import com.foodya.backend.application.dto.OrderPaymentModel;
import com.foodya.backend.application.dto.RestaurantModel;
import com.foodya.backend.application.ports.out.OrderEventPublisherPort;
import com.foodya.backend.application.ports.out.OrderCheckoutPort;
import com.foodya.backend.application.ports.out.RouteDistancePort;
import com.foodya.backend.application.ports.out.SystemParameterPort;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.value_objects.PaymentMethod;
import com.foodya.backend.domain.value_objects.PaymentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderCheckoutService implements OrderCheckoutUseCase {

    private final OrderCheckoutPort orderCheckoutPort;
    private final RouteDistancePort routeDistancePort;
    private final SystemParameterPort systemParameterPort;
    private final OrderEventPublisherPort orderEventPublisherPort;

    public OrderCheckoutService(OrderCheckoutPort orderCheckoutPort,
                                RouteDistancePort routeDistancePort,
                                SystemParameterPort systemParameterPort,
                                OrderEventPublisherPort orderEventPublisherPort) {
        this.orderCheckoutPort = orderCheckoutPort;
        this.routeDistancePort = routeDistancePort;
        this.systemParameterPort = systemParameterPort;
        this.orderEventPublisherPort = orderEventPublisherPort;
    }

    @Transactional
    public OrderCreatedView createOrder(UUID customerUserId,
                                        String idempotencyKey,
                                        CreateOrderRequest request) {
        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);

        OrderModel existing = orderCheckoutPort.findByCustomerUserIdAndIdempotencyKey(customerUserId, normalizedIdempotencyKey).orElse(null);
        if (existing != null) {
            return toView(existing);
        }

        UUID restaurantId = parseUuid(request.restaurantId(), "restaurantId");
        RestaurantModel restaurant = orderCheckoutPort.findActiveRestaurantById(restaurantId)
                .orElseThrow(() -> new NotFoundException("restaurant not found"));

        if (request.items() == null || request.items().isEmpty()) {
            throw new ValidationException("order items are required", Map.of("items", "must not be empty"));
        }

        int currencyMinorUnit = intParam("currency.minor_unit", 0);
        RoundingMode roundingMode = roundingModeParam("currency.rounding_mode", "HALF_UP");

        List<OrderItemModel> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CreateOrderItemRequest itemRequest : request.items()) {
            UUID menuItemId = parseUuid(itemRequest.menuItemId(), "menuItemId");
                MenuItemModel menuItem = orderCheckoutPort.findMenuItemById(menuItemId)
                    .orElseThrow(() -> new NotFoundException("menu item not found"));

            if (!restaurantId.equals(menuItem.getRestaurantId())) {
                throw new ValidationException("multi-restaurant order is not allowed", Map.of("items", "all items must belong to one restaurant"));
            }
            assertMenuItemOrderable(menuItem);

            BigDecimal lineTotal = round(menuItem.getPrice().multiply(BigDecimal.valueOf(itemRequest.quantity())), currencyMinorUnit, roundingMode);
            subtotal = subtotal.add(lineTotal);

            OrderItemModel line = new OrderItemModel();
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

        OrderModel order = new OrderModel();
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

        OrderModel savedOrder = orderCheckoutPort.saveOrder(order);

        for (OrderItemModel item : orderItems) {
            item.setOrderId(savedOrder.getId());
        }
        orderCheckoutPort.saveOrderItems(orderItems);

        OrderPaymentModel payment = new OrderPaymentModel();
        payment.setOrderId(savedOrder.getId());
        payment.setPaymentMethod(PaymentMethod.COD);
        payment.setPaymentStatus(PaymentStatus.UNPAID);
        payment.setAmount(savedOrder.getTotalAmount());
        orderCheckoutPort.saveOrderPayment(payment);

        orderEventPublisherPort.publishOrderNotification(new OrderNotificationEvent(
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
                .map(p -> p.getValue())
                .map(BigDecimal::new)
                .orElse(fallback);
    }

    private int intParam(String key, int fallback) {
        return systemParameterPort.findById(key)
                .map(p -> p.getValue())
                .map(Integer::parseInt)
                .orElse(fallback);
    }

    private RoundingMode roundingModeParam(String key, String fallback) {
        String value = systemParameterPort.findById(key)
                .map(p -> p.getValue())
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

    private static void assertMenuItemOrderable(MenuItemModel menuItem) {
        if (!menuItem.isActive() || !menuItem.isAvailable() || menuItem.getDeletedAt() != null) {
            throw new ValidationException("menu item is not orderable", Map.of("menuItemId", "inactive or unavailable"));
        }
    }

    private static String newOrderCode() {
        String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "ORD-" + timestamp + "-" + suffix;
    }

    private OrderCreatedView toView(OrderModel order) {
        String currencyCode = systemParameterPort.findById("currency.code")
                .map(p -> p.getValue())
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
