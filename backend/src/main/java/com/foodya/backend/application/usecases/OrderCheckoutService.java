package com.foodya.backend.application.usecases;

import com.foodya.backend.application.dto.CreateOrderFromCartRequest;
import com.foodya.backend.application.dto.OrderCostReviewView;
import com.foodya.backend.application.dto.OrderCreatedView;
import com.foodya.backend.application.event.OrderNotificationEvent;
import com.foodya.backend.application.exception.NotFoundException;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.ports.in.OrderCheckoutUseCase;
import com.foodya.backend.application.ports.out.CartItemPort;
import com.foodya.backend.application.ports.out.CartPort;
import com.foodya.backend.application.dto.MenuItemData;
import com.foodya.backend.application.dto.OrderItemData;
import com.foodya.backend.application.dto.OrderData;
import com.foodya.backend.application.dto.OrderPaymentData;
import com.foodya.backend.application.dto.RestaurantData;
import com.foodya.backend.application.ports.out.OrderEventPublisherPort;
import com.foodya.backend.application.ports.out.OrderCheckoutPort;
import com.foodya.backend.application.ports.out.RouteDistancePort;
import com.foodya.backend.application.ports.out.SystemParameterPort;
import com.foodya.backend.domain.entities.CartItem;
import com.foodya.backend.domain.value_objects.CartStatus;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.value_objects.PaymentMethod;
import com.foodya.backend.domain.value_objects.PaymentStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class OrderCheckoutService implements OrderCheckoutUseCase {

    private final OrderCheckoutPort orderCheckoutPort;
    private final CartPort cartPort;
    private final CartItemPort cartItemPort;
    private final RouteDistancePort routeDistancePort;
    private final SystemParameterPort systemParameterPort;
    private final OrderEventPublisherPort orderEventPublisherPort;

    public OrderCheckoutService(OrderCheckoutPort orderCheckoutPort,
                                CartPort cartPort,
                                CartItemPort cartItemPort,
                                RouteDistancePort routeDistancePort,
                                SystemParameterPort systemParameterPort,
                                OrderEventPublisherPort orderEventPublisherPort) {
        this.orderCheckoutPort = orderCheckoutPort;
        this.cartPort = cartPort;
        this.cartItemPort = cartItemPort;
        this.routeDistancePort = routeDistancePort;
        this.systemParameterPort = systemParameterPort;
        this.orderEventPublisherPort = orderEventPublisherPort;
    }

    public OrderCostReviewView reviewCurrentCartCost(UUID customerUserId, CreateOrderFromCartRequest request) {
        CartCheckoutQuote quote = buildCartCheckoutQuote(customerUserId, request);
        return new OrderCostReviewView(
                quote.subtotal,
                quote.deliveryFee,
                quote.total,
                quote.commissionAmount,
                quote.shippingFeeMarginAmount,
                quote.platformProfitAmount,
                currencyCode()
        );
    }

    public OrderCreatedView createOrder(UUID customerUserId,
                                        String idempotencyKey,
                                        CreateOrderFromCartRequest request) {
        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);

        OrderData existing = orderCheckoutPort.findByCustomerUserIdAndIdempotencyKey(customerUserId, normalizedIdempotencyKey).orElse(null);
        if (existing != null) {
            return toView(existing);
        }

        CartCheckoutQuote quote = buildCartCheckoutQuote(customerUserId, request);

        OrderData order = new OrderData();
        order.setOrderCode(newOrderCode());
        order.setCustomerUserId(customerUserId);
        order.setIdempotencyKey(normalizedIdempotencyKey);
        order.setRestaurantId(quote.restaurant.getId());
        order.setStatus(OrderStatus.PENDING);
        order.setDeliveryAddress(request.deliveryAddress().trim());
        order.setDeliveryLatitude(request.deliveryLatitude());
        order.setDeliveryLongitude(request.deliveryLongitude());
        order.setCustomerNote(request.customerNote());
        order.setSubtotalAmount(quote.subtotal);
        order.setDeliveryFee(quote.deliveryFee);
        order.setTotalAmount(quote.total);
        order.setPaymentMethod(PaymentMethod.COD);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setCommissionAmount(quote.commissionAmount);
        order.setShippingFeeMarginAmount(quote.shippingFeeMarginAmount);
        order.setPlatformProfitAmount(quote.platformProfitAmount);

        OrderData savedOrder = orderCheckoutPort.saveOrder(order);

        for (OrderItemData item : quote.orderItems) {
            item.setOrderId(savedOrder.getId());
        }
        orderCheckoutPort.saveOrderItems(quote.orderItems);

        OrderPaymentData payment = new OrderPaymentData();
        payment.setOrderId(savedOrder.getId());
        payment.setPaymentMethod(PaymentMethod.COD);
        payment.setPaymentStatus(PaymentStatus.UNPAID);
        payment.setAmount(savedOrder.getTotalAmount());
        orderCheckoutPort.saveOrderPayment(payment);

        orderEventPublisherPort.publishOrderNotification(new OrderNotificationEvent(
            savedOrder.getId(),
            savedOrder.getOrderCode(),
            savedOrder.getCustomerUserId(),
            quote.restaurant.getOwnerUserId(),
            savedOrder.getStatus(),
            "ORDER_CREATED",
            "New order " + savedOrder.getOrderCode(),
            "Order placed and awaiting merchant acceptance"
        ));

        return toView(savedOrder);
    }

    private CartCheckoutQuote buildCartCheckoutQuote(UUID customerUserId, CreateOrderFromCartRequest request) {
        requireText(request.deliveryAddress(), "deliveryAddress");
        requireNonNull(request.deliveryLatitude(), "deliveryLatitude");
        requireNonNull(request.deliveryLongitude(), "deliveryLongitude");

        var activeCart = cartPort.findByCustomerUserIdAndStatus(customerUserId, CartStatus.ACTIVE)
                .orElseThrow(() -> new ValidationException("active cart is required", Map.of("cart", "active cart not found")));

        List<CartItem> cartItems = cartItemPort.findByCartId(activeCart.getId());
        if (cartItems.isEmpty()) {
            throw new ValidationException("order items are required", Map.of("items", "current cart must not be empty"));
        }

        UUID restaurantId = activeCart.getRestaurantId();
        if (restaurantId == null) {
            UUID menuItemId = cartItems.get(0).getMenuItemId();
            MenuItemData firstMenuItem = orderCheckoutPort.findMenuItemById(menuItemId)
                    .orElseThrow(() -> new NotFoundException("menu item not found"));
            restaurantId = firstMenuItem.getRestaurantId();
        }

        RestaurantData restaurant = orderCheckoutPort.findActiveRestaurantById(restaurantId)
                .orElseThrow(() -> new NotFoundException("restaurant not found"));

        int currencyMinorUnit = intParam("currency.minor_unit", 0);
        RoundingMode roundingMode = roundingModeParam("currency.rounding_mode", "HALF_UP");

        List<OrderItemData> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            if (cartItem.getQuantity() <= 0) {
                throw new ValidationException("invalid quantity", Map.of("quantity", "must be >= 1"));
            }

            MenuItemData menuItem = orderCheckoutPort.findMenuItemById(cartItem.getMenuItemId())
                    .orElseThrow(() -> new NotFoundException("menu item not found"));

            if (!restaurantId.equals(menuItem.getRestaurantId())) {
                throw new ValidationException("multi-restaurant order is not allowed", Map.of("items", "all items must belong to one restaurant"));
            }
            assertMenuItemOrderable(menuItem);

            BigDecimal lineTotal = round(menuItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())), currencyMinorUnit, roundingMode);
            subtotal = subtotal.add(lineTotal);

            OrderItemData line = new OrderItemData();
            line.setMenuItemId(menuItem.getId());
            line.setMenuItemNameSnapshot(menuItem.getName());
            line.setUnitPriceSnapshot(menuItem.getPrice());
            line.setQuantity(cartItem.getQuantity());
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
        BigDecimal roundedSubtotal = round(subtotal, currencyMinorUnit, roundingMode);
        BigDecimal total = round(roundedSubtotal.add(deliveryFee), currencyMinorUnit, roundingMode);

        BigDecimal commissionRatePercent = decimalParam("finance.commission_rate_percent", BigDecimal.TEN);
        BigDecimal shippingMarginRatePercent = decimalParam("finance.shipping_margin_rate_percent", BigDecimal.ZERO);

        BigDecimal commissionAmount = round(
                roundedSubtotal.multiply(commissionRatePercent).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP),
                currencyMinorUnit,
                roundingMode
        );
        BigDecimal shippingFeeMarginAmount = round(
                deliveryFee.multiply(shippingMarginRatePercent).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP),
                currencyMinorUnit,
                roundingMode
        );
        BigDecimal platformProfitAmount = round(commissionAmount.add(shippingFeeMarginAmount), currencyMinorUnit, roundingMode);

        return new CartCheckoutQuote(
                restaurant,
                orderItems,
                roundedSubtotal,
                deliveryFee,
                total,
                commissionAmount,
                shippingFeeMarginAmount,
                platformProfitAmount
        );
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

    private static void assertMenuItemOrderable(MenuItemData menuItem) {
        if (!menuItem.isActive() || !menuItem.isAvailable() || menuItem.getDeletedAt() != null) {
            throw new ValidationException("menu item is not orderable", Map.of("menuItemId", "inactive or unavailable"));
        }
    }

    private static String newOrderCode() {
        String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "ORD-" + timestamp + "-" + suffix;
    }

    private String currencyCode() {
        return systemParameterPort.findById("currency.code")
                .map(p -> p.getValue())
                .orElse("VND");
    }

    private OrderCreatedView toView(OrderData order) {
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
                currencyCode()
        );
    }

    private static final class CartCheckoutQuote {
        private final RestaurantData restaurant;
        private final List<OrderItemData> orderItems;
        private final BigDecimal subtotal;
        private final BigDecimal deliveryFee;
        private final BigDecimal total;
        private final BigDecimal commissionAmount;
        private final BigDecimal shippingFeeMarginAmount;
        private final BigDecimal platformProfitAmount;

        private CartCheckoutQuote(RestaurantData restaurant,
                                  List<OrderItemData> orderItems,
                                  BigDecimal subtotal,
                                  BigDecimal deliveryFee,
                                  BigDecimal total,
                                  BigDecimal commissionAmount,
                                  BigDecimal shippingFeeMarginAmount,
                                  BigDecimal platformProfitAmount) {
            this.restaurant = Objects.requireNonNull(restaurant);
            this.orderItems = List.copyOf(orderItems);
            this.subtotal = subtotal;
            this.deliveryFee = deliveryFee;
            this.total = total;
            this.commissionAmount = commissionAmount;
            this.shippingFeeMarginAmount = shippingFeeMarginAmount;
            this.platformProfitAmount = platformProfitAmount;
        }
    }
}
