package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.ports.out.GeoPort;
import com.foodya.backend.application.ports.out.TokenPort;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.infrastructure.mapper.AuthPersistenceMapper;
import com.foodya.backend.domain.value_objects.PaymentMethod;
import com.foodya.backend.domain.value_objects.PaymentStatus;
import com.foodya.backend.domain.value_objects.RestaurantStatus;
import com.foodya.backend.domain.value_objects.UserRole;
import com.foodya.backend.domain.value_objects.UserStatus;
import com.foodya.backend.domain.entities.Order;
import com.foodya.backend.domain.entities.Restaurant;
import com.foodya.backend.domain.entities.UserAccount;
import com.foodya.backend.infrastructure.repository.DeliveryTrackingPointRepository;
import com.foodya.backend.infrastructure.repository.CartItemRepository;
import com.foodya.backend.infrastructure.repository.CartRepository;
import com.foodya.backend.infrastructure.repository.MenuCategoryRepository;
import com.foodya.backend.infrastructure.repository.MenuItemRepository;
import com.foodya.backend.infrastructure.repository.OrderManagementRepository;
import com.foodya.backend.infrastructure.repository.OrderPaymentRepository;
import com.foodya.backend.infrastructure.repository.OrderRepository;
import com.foodya.backend.infrastructure.repository.RestaurantRepository;
import com.foodya.backend.infrastructure.repository.UserAccountRepository;
import com.foodya.backend.infrastructure.mapper.OrderMapper;
import com.foodya.backend.infrastructure.mapper.RestaurantMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import java.util.Objects;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderLifecycleIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private MenuCategoryRepository menuCategoryRepository;

    @Autowired
    private OrderManagementRepository orderManagementRepository;

    @Autowired
    private OrderPaymentRepository orderPaymentRepository;

    @Autowired
    private DeliveryTrackingPointRepository deliveryTrackingPointRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private TokenPort tokenService;

    @Autowired
    private GeoPort geoService;

    @Autowired
    private RestaurantMapper restaurantMapper;

    private UserAccount driver;

    @BeforeEach
    void setUp() {
        deliveryTrackingPointRepository.deleteAll();
        orderPaymentRepository.deleteAll();
        orderManagementRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        menuItemRepository.deleteAll();
        menuCategoryRepository.deleteAll();
        restaurantRepository.deleteAll();
        userAccountRepository.deleteAll();
        driver = seedUser("integration-driver", UserRole.DELIVERY);
    }

    @Test
    void customerCanViewAndCancelOwnOrder() throws Exception {
        UserAccount customer = seedUser("customer-a", UserRole.CUSTOMER);
        UserAccount merchant = seedUser("merchant-a", UserRole.MERCHANT);
        Restaurant restaurant = seedRestaurant(merchant, "Cancel Store");
        Order order = seedOrder(customer, restaurant, OrderStatus.PENDING);

        String customerToken = tokenService.issueAccessToken(customer, UUID.randomUUID().toString());

        mockMvc.perform(get("/api/v1/customer/orders")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].orderId").value(order.getId().toString()));

        mockMvc.perform(post("/api/v1/customer/orders/{id}/cancel", order.getId())
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("{\"reason\":\"change plan\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void deliveryFlowSupportsTrackingAndCustomerRead() throws Exception {
        UserAccount customer = seedUser("customer-b", UserRole.CUSTOMER);
        UserAccount merchant = seedUser("merchant-b", UserRole.MERCHANT);
        UserAccount delivery = driver;
        Restaurant restaurant = seedRestaurant(merchant, "Delivery Store");
        Order order = seedOrder(customer, restaurant, OrderStatus.ASSIGNED);

        String deliveryToken = tokenService.issueAccessToken(delivery, UUID.randomUUID().toString());
        String customerToken = tokenService.issueAccessToken(customer, UUID.randomUUID().toString());

        mockMvc.perform(post("/api/v1/delivery/orders/{id}/accept", order.getId())
                        .header("Authorization", "Bearer " + deliveryToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"));

        mockMvc.perform(post("/api/v1/delivery/orders/{id}/tracking-points", order.getId())
                        .header("Authorization", "Bearer " + deliveryToken)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("{\"lat\":10.7800000,\"lng\":106.7000000,\"recordedAt\":\"2026-03-31T00:00:00Z\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.lat").value(10.78));

        mockMvc.perform(get("/api/v1/customer/orders/{id}/tracking", order.getId())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].lng").value(106.7));
    }

    @Test
    void deliverySuccessMarksCodOrderPaidAndMirrorsPaymentRow() throws Exception {
        UserAccount customer = seedUser("customer-c", UserRole.CUSTOMER);
        UserAccount merchant = seedUser("merchant-c", UserRole.MERCHANT);
        UserAccount delivery = driver;
        Restaurant restaurant = seedRestaurant(merchant, "Payment Store");
        Order order = seedOrder(customer, restaurant, OrderStatus.ASSIGNED);

        String deliveryToken = tokenService.issueAccessToken(delivery, UUID.randomUUID().toString());

        mockMvc.perform(post("/api/v1/delivery/orders/{id}/accept", order.getId())
                        .header("Authorization", "Bearer " + deliveryToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.data.paymentStatus").value("UNPAID"));

        mockMvc.perform(patch("/api/v1/delivery/orders/{id}/status", order.getId())
                        .header("Authorization", "Bearer " + deliveryToken)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("{\"status\":\"DELIVERING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELIVERING"))
                .andExpect(jsonPath("$.data.paymentStatus").value("UNPAID"));

        mockMvc.perform(patch("/api/v1/delivery/orders/{id}/status", order.getId())
                        .header("Authorization", "Bearer " + deliveryToken)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("{\"status\":\"SUCCESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PAID"));

        var savedOrder = orderRepository.findById(order.getId()).orElseThrow();
        var payment = orderPaymentRepository.findByOrderId(order.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(PaymentStatus.PAID, savedOrder.getPaymentStatus());
        org.junit.jupiter.api.Assertions.assertEquals(savedOrder.getPaymentStatus(), payment.getPaymentStatus());
        org.junit.jupiter.api.Assertions.assertEquals(PaymentMethod.COD, payment.getPaymentMethod());
        org.junit.jupiter.api.Assertions.assertEquals(0, savedOrder.getTotalAmount().compareTo(payment.getAmount()));
        org.junit.jupiter.api.Assertions.assertNotNull(payment.getPaidAt());
    }

    private UserAccount seedUser(String stem, UserRole role) {
        com.foodya.backend.infrastructure.persistence.models.UserAccountPersistenceModel user = new com.foodya.backend.infrastructure.persistence.models.UserAccountPersistenceModel();
        user.setUsername(stem);
        user.setEmail(stem + "@test.local");
        user.setPhoneNumber("+8490" + Math.abs(stem.hashCode() % 10000000));
        user.setFullName(stem);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash("$2a$10$abcdefghijklmnopqrstuv");
        return new com.foodya.backend.infrastructure.mapper.UserAccountMapper().toDomain(userAccountRepository.save(user));
    }

    private Restaurant seedRestaurant(UserAccount owner, String name) {
        Restaurant restaurant = new Restaurant();
        restaurant.setOwnerUserId(owner.getId());
        restaurant.setName(name);
        restaurant.setCuisineType("Vietnamese");
        restaurant.setDescription(name + " desc");
        restaurant.setAddressLine("123 Test Street");
        restaurant.setLatitude(new BigDecimal("10.7700000"));
        restaurant.setLongitude(new BigDecimal("106.7000000"));
        restaurant.setH3IndexRes9(geoService.h3Res9(10.77, 106.7));
        restaurant.setStatus(RestaurantStatus.ACTIVE);
        restaurant.setOpen(true);
        restaurant.setMaxDeliveryKm(new BigDecimal("8.0"));
        var persistenceModel = restaurantMapper.toPersistence(restaurant);
        @SuppressWarnings("null")
        var saved = restaurantRepository.save(persistenceModel);
        return restaurantMapper.toDomain(saved);
    }

    private Order seedOrder(UserAccount customer, Restaurant restaurant, OrderStatus status) {
        Order order = new Order();
        if (status == OrderStatus.SUCCESS) {
            order.setCompletedAt(java.time.OffsetDateTime.now());
        }
        if (status != OrderStatus.PENDING && status != OrderStatus.ACCEPTED) {
            order.setDriverUserId(driver.getId());
        }
        order.setOrderCode("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setCustomerUserId(customer.getId());
        order.setIdempotencyKey(UUID.randomUUID().toString());
        order.setRestaurantId(restaurant.getId());
        order.setStatus(status);
        order.setDeliveryAddress("10 Main St");
        order.setDeliveryLatitude(new BigDecimal("10.7800000"));
        order.setDeliveryLongitude(new BigDecimal("106.7100000"));
        order.setCustomerNote("none");
        order.setSubtotalAmount(new BigDecimal("50000.00"));
        order.setDeliveryFee(new BigDecimal("10000.00"));
        order.setTotalAmount(new BigDecimal("60000.00"));
        order.setPaymentMethod(PaymentMethod.COD);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setCommissionAmount(new BigDecimal("5000.00"));
        order.setShippingFeeMarginAmount(new BigDecimal("0.00"));
        order.setPlatformProfitAmount(new BigDecimal("5000.00"));
        var persistenceModel = orderMapper.toPersistence(order);
        @SuppressWarnings("null")
        var saved = orderRepository.save(persistenceModel);
        return orderMapper.toDomain(saved);
    }

    @Test
    void merchantCanRejectAnOrderItCannotFulfil() throws Exception {
        UserAccount customer = seedUser("customer-rej", UserRole.CUSTOMER);
        UserAccount merchant = seedUser("merchant-rej", UserRole.MERCHANT);
        Restaurant restaurant = seedRestaurant(merchant, "Reject Store");
        Order order = seedOrder(customer, restaurant, OrderStatus.PENDING);
        String merchantToken = tokenService.issueAccessToken(merchant, UUID.randomUUID().toString());
        String customerToken = tokenService.issueAccessToken(customer, UUID.randomUUID().toString());

        mockMvc.perform(post("/api/v1/merchant/orders/{id}/reject", order.getId())
                        .header("Authorization", "Bearer " + merchantToken)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("{\"reason\":\"out of stock\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        mockMvc.perform(get("/api/v1/customer/orders/{id}", order.getId())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void anotherDriverCannotActOnAnOrder() throws Exception {
        UserAccount customer = seedUser("customer-authz", UserRole.CUSTOMER);
        UserAccount merchant = seedUser("merchant-authz", UserRole.MERCHANT);
        Restaurant restaurant = seedRestaurant(merchant, "Authz Store");
        Order order = seedOrder(customer, restaurant, OrderStatus.ASSIGNED);
        UserAccount intruder = seedUser("driver-intruder", UserRole.DELIVERY);
        String intruderToken = tokenService.issueAccessToken(intruder, UUID.randomUUID().toString());

        mockMvc.perform(patch("/api/v1/delivery/orders/{id}/status", order.getId())
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("{\"status\":\"DELIVERING\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/delivery/orders/{id}/tracking-points", order.getId())
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("{\"lat\":10.78,\"lng\":106.70,\"recordedAt\":\"2026-03-31T00:00:00Z\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignedDriverCanDeclineAndOrderReturnsToThePool() throws Exception {
        UserAccount customer = seedUser("customer-decline", UserRole.CUSTOMER);
        UserAccount merchant = seedUser("merchant-decline", UserRole.MERCHANT);
        Restaurant restaurant = seedRestaurant(merchant, "Decline Store");
        Order order = seedOrder(customer, restaurant, OrderStatus.ASSIGNED);
        String deliveryToken = tokenService.issueAccessToken(driver, UUID.randomUUID().toString());

        mockMvc.perform(post("/api/v1/delivery/orders/{id}/reject", order.getId())
                        .header("Authorization", "Bearer " + deliveryToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        var saved = orderRepository.findById(order.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertNull(saved.getDriverUserId());
    }

    @Test
    void driverEarningsCreditTheDeliveryFeeMinusPlatformMargin() throws Exception {
        UserAccount customer = seedUser("customer-earn", UserRole.CUSTOMER);
        UserAccount merchant = seedUser("merchant-earn", UserRole.MERCHANT);
        Restaurant restaurant = seedRestaurant(merchant, "Earnings Store");
        Order order = seedOrder(customer, restaurant, OrderStatus.SUCCESS);
        orderRepository.save(orderMapper.toPersistence(order));
        String deliveryToken = tokenService.issueAccessToken(driver, UUID.randomUUID().toString());

        mockMvc.perform(get("/api/v1/delivery/earnings").header("Authorization", "Bearer " + deliveryToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deliveryCount").value(1))
                .andExpect(jsonPath("$.data.totalEarnings").value(10000))
                .andExpect(jsonPath("$.data.codCashCollected").value(60000));
    }

    @Test
    void aDriverOnlySeesOrdersTheyHold() throws Exception {
        UserAccount customer = seedUser("customer-assign", UserRole.CUSTOMER);
        UserAccount merchant = seedUser("merchant-assign", UserRole.MERCHANT);
        Restaurant restaurant = seedRestaurant(merchant, "Assign Store");
        Order mine = seedOrder(customer, restaurant, OrderStatus.ASSIGNED);
        Order alsoMine = seedOrder(customer, restaurant, OrderStatus.DELIVERING);
        seedOrder(customer, restaurant, OrderStatus.ACCEPTED);      // not yet assigned to anyone
        String deliveryToken = tokenService.issueAccessToken(driver, UUID.randomUUID().toString());

        String body = mockMvc.perform(get("/api/v1/delivery/orders/assignments")
                        .header("Authorization", "Bearer " + deliveryToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var ids = new java.util.ArrayList<String>();
        for (var node : new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("data")) {
            ids.add(node.get("orderId").asText());
        }
        org.junit.jupiter.api.Assertions.assertTrue(ids.contains(mine.getId().toString()));
        org.junit.jupiter.api.Assertions.assertTrue(ids.contains(alsoMine.getId().toString()));
        org.junit.jupiter.api.Assertions.assertEquals(2, ids.size(),
                "unassigned ACCEPTED orders must not be listed as the driver's assignments");
    }

    @Test
    void anotherDriversOrdersAreNotListed() throws Exception {
        UserAccount customer = seedUser("customer-foreign", UserRole.CUSTOMER);
        UserAccount merchant = seedUser("merchant-foreign", UserRole.MERCHANT);
        Restaurant restaurant = seedRestaurant(merchant, "Foreign Store");
        Order heldBySomeoneElse = seedOrder(customer, restaurant, OrderStatus.ASSIGNED);
        UserAccount otherDriver = seedUser("driver-other", UserRole.DELIVERY);
        heldBySomeoneElse.setDriverUserId(otherDriver.getId());
        orderRepository.save(orderMapper.toPersistence(heldBySomeoneElse));
        String driverToken = tokenService.issueAccessToken(driver, UUID.randomUUID().toString());

        mockMvc.perform(get("/api/v1/delivery/orders/assignments")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}
