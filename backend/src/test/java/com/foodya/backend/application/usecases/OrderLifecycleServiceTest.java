package com.foodya.backend.application.usecases;

import com.foodya.backend.application.dto.OrderDetailView;
import com.foodya.backend.application.ports.out.DeliveryTrackingPointPort;
import com.foodya.backend.application.ports.out.DriverMatchingPort;
import com.foodya.backend.application.ports.out.DriverOnlineSessionPort;
import com.foodya.backend.application.ports.out.OrderEventPublisherPort;
import com.foodya.backend.application.ports.out.OrderManagementPort;
import com.foodya.backend.application.ports.out.OrderPaymentPort;
import com.foodya.backend.application.ports.out.OrderTrackingUpdatePublisherPort;
import com.foodya.backend.application.ports.out.RestaurantPort;
import com.foodya.backend.application.ports.out.UserAccountPort;
import com.foodya.backend.application.support.PaginationPolicy;
import com.foodya.backend.domain.entities.DriverOnlineSession;
import com.foodya.backend.domain.entities.Order;
import com.foodya.backend.domain.entities.Restaurant;
import com.foodya.backend.domain.value_objects.DriverMatchRequest;
import com.foodya.backend.domain.value_objects.DriverMatchResult;
import com.foodya.backend.domain.value_objects.DriverSessionStatus;
import com.foodya.backend.domain.value_objects.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderLifecycleServiceTest {

    private OrderManagementPort orderManagementPort;
    private RestaurantPort restaurantPort;
    private DeliveryTrackingPointPort deliveryTrackingPointPort;
    private OrderEventPublisherPort orderEventPublisherPort;
    private UserAccountPort userAccountPort;
    private OrderPaymentPort orderPaymentPort;
    private OrderTrackingUpdatePublisherPort orderTrackingUpdatePublisherPort;
    private PaginationPolicy paginationPolicy;
    private DriverMatchingPort driverMatchingPort;
    private DriverOnlineSessionPort driverOnlineSessionPort;

    private OrderLifecycleService service;

    @BeforeEach
    void setUp() {
        orderManagementPort = mock(OrderManagementPort.class);
        restaurantPort = mock(RestaurantPort.class);
        deliveryTrackingPointPort = mock(DeliveryTrackingPointPort.class);
        orderEventPublisherPort = mock(OrderEventPublisherPort.class);
        userAccountPort = mock(UserAccountPort.class);
        orderPaymentPort = mock(OrderPaymentPort.class);
        orderTrackingUpdatePublisherPort = mock(OrderTrackingUpdatePublisherPort.class);
        paginationPolicy = mock(PaginationPolicy.class);
        driverMatchingPort = mock(DriverMatchingPort.class);
        driverOnlineSessionPort = mock(DriverOnlineSessionPort.class);

        service = new OrderLifecycleService(
                orderManagementPort,
                restaurantPort,
                deliveryTrackingPointPort,
                orderEventPublisherPort,
                userAccountPort,
                orderPaymentPort,
                orderTrackingUpdatePublisherPort,
                paginationPolicy,
                driverMatchingPort,
                driverOnlineSessionPort
        );
    }

    @Test
    @DisplayName("Merchant updates status to ACCEPTED and auto-assigns matching driver")
    void testMerchantAccept_WithDriverMatch() {
        UUID merchantId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID driverUserId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        Restaurant restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setOwnerUserId(merchantId);
        restaurant.setLatitude(BigDecimal.valueOf(10.776));
        restaurant.setLongitude(BigDecimal.valueOf(106.700));

        Order order = new Order();
        order.setId(orderId);
        order.setOrderCode("ORD-1001");
        order.setRestaurantId(restaurantId);
        order.setCustomerUserId(UUID.randomUUID());
        order.setDeliveryLatitude(BigDecimal.valueOf(10.780));
        order.setDeliveryLongitude(BigDecimal.valueOf(106.710));
        order.setTotalAmount(BigDecimal.valueOf(100000));
        order.setPlacedAt(OffsetDateTime.now());
        // Initial state is PENDING
        order.setStatus(OrderStatus.PENDING);

        when(orderManagementPort.findById(orderId)).thenReturn(Optional.of(order));
        when(restaurantPort.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(orderManagementPort.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DriverMatchResult matchResult = new DriverMatchResult(driverUserId, sessionId, 0.5, 0.85);
        when(driverMatchingPort.findBestDriver(any(DriverMatchRequest.class))).thenReturn(Optional.of(matchResult));

        DriverOnlineSession session = new DriverOnlineSession();
        session.setId(sessionId);
        session.setDriverUserId(driverUserId);
        session.setStatus(DriverSessionStatus.ONLINE);
        session.setMaxConcurrentOrders(2);
        session.setActiveOrderCount(0);
        session.setLastHeartbeat(Instant.now());

        when(driverOnlineSessionPort.findActiveSession(driverUserId)).thenReturn(Optional.of(session));

        OrderDetailView detailView = service.merchantUpdateStatus(merchantId, orderId, OrderStatus.ACCEPTED);

        assertThat(detailView).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ASSIGNED);
        assertThat(order.getDriverUserId()).isEqualTo(driverUserId);
        assertThat(session.getActiveOrderCount()).isEqualTo(1);
        assertThat(session.getStatus()).isEqualTo(DriverSessionStatus.ON_DELIVERY);

        verify(driverOnlineSessionPort).save(session);
    }

    @Test
    @DisplayName("Merchant updates status to ACCEPTED when no driver matched — order stays ACCEPTED")
    void testMerchantAccept_NoDriverMatch() {
        UUID merchantId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        Restaurant restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setOwnerUserId(merchantId);
        restaurant.setLatitude(BigDecimal.valueOf(10.776));
        restaurant.setLongitude(BigDecimal.valueOf(106.700));

        Order order = new Order();
        order.setId(orderId);
        order.setOrderCode("ORD-1002");
        order.setRestaurantId(restaurantId);
        order.setCustomerUserId(UUID.randomUUID());
        order.setDeliveryLatitude(BigDecimal.valueOf(10.780));
        order.setDeliveryLongitude(BigDecimal.valueOf(106.710));
        order.setTotalAmount(BigDecimal.valueOf(100000));
        order.setPlacedAt(OffsetDateTime.now());
        order.setStatus(OrderStatus.PENDING);

        when(orderManagementPort.findById(orderId)).thenReturn(Optional.of(order));
        when(restaurantPort.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(orderManagementPort.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(driverMatchingPort.findBestDriver(any(DriverMatchRequest.class))).thenReturn(Optional.empty());

        OrderDetailView detailView = service.merchantUpdateStatus(merchantId, orderId, OrderStatus.ACCEPTED);

        assertThat(detailView).isNotNull();
        // Order accepted, but driver matching returned empty, so order stays ACCEPTED
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(order.getDriverUserId()).isNull();
    }
}
