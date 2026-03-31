package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.service.GeoService;
import com.foodya.backend.application.service.TokenService;
import com.foodya.backend.domain.model.OrderStatus;
import com.foodya.backend.domain.model.PaymentMethod;
import com.foodya.backend.domain.model.PaymentStatus;
import com.foodya.backend.domain.model.RestaurantStatus;
import com.foodya.backend.domain.model.UserRole;
import com.foodya.backend.domain.model.UserStatus;
import com.foodya.backend.domain.persistence.Order;
import com.foodya.backend.domain.persistence.Restaurant;
import com.foodya.backend.domain.persistence.UserAccount;
import com.foodya.backend.infrastructure.repository.DeliveryTrackingPointRepository;
import com.foodya.backend.infrastructure.repository.CartItemRepository;
import com.foodya.backend.infrastructure.repository.CartRepository;
import com.foodya.backend.infrastructure.repository.MenuCategoryRepository;
import com.foodya.backend.infrastructure.repository.MenuItemRepository;
import com.foodya.backend.infrastructure.repository.OrderManagementRepository;
import com.foodya.backend.infrastructure.repository.OrderRepository;
import com.foodya.backend.infrastructure.repository.RestaurantRepository;
import com.foodya.backend.infrastructure.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    private DeliveryTrackingPointRepository deliveryTrackingPointRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private GeoService geoService;

    @BeforeEach
    void setUp() {
        deliveryTrackingPointRepository.deleteAll();
        orderManagementRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        menuItemRepository.deleteAll();
        menuCategoryRepository.deleteAll();
        restaurantRepository.deleteAll();
        userAccountRepository.deleteAll();
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"change plan\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void deliveryFlowSupportsTrackingAndCustomerRead() throws Exception {
        UserAccount customer = seedUser("customer-b", UserRole.CUSTOMER);
        UserAccount merchant = seedUser("merchant-b", UserRole.MERCHANT);
        UserAccount delivery = seedUser("delivery-b", UserRole.DELIVERY);
        Restaurant restaurant = seedRestaurant(merchant, "Delivery Store");
        Order order = seedOrder(customer, restaurant, OrderStatus.ACCEPTED);

        String deliveryToken = tokenService.issueAccessToken(delivery, UUID.randomUUID().toString());
        String customerToken = tokenService.issueAccessToken(customer, UUID.randomUUID().toString());

        mockMvc.perform(post("/api/v1/delivery/orders/{id}/accept", order.getId())
                        .header("Authorization", "Bearer " + deliveryToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"));

        mockMvc.perform(post("/api/v1/delivery/orders/{id}/tracking-points", order.getId())
                        .header("Authorization", "Bearer " + deliveryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lat\":10.7800000,\"lng\":106.7000000,\"recordedAt\":\"2026-03-31T00:00:00Z\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lat").value(10.78));

        mockMvc.perform(get("/api/v1/customer/orders/{id}/tracking", order.getId())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].lng").value(106.7));
    }

    private UserAccount seedUser(String stem, UserRole role) {
        UserAccount user = new UserAccount();
        user.setUsername(stem);
        user.setEmail(stem + "@test.local");
        user.setPhoneNumber("+8490" + Math.abs(stem.hashCode() % 10000000));
        user.setFullName(stem);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash("$2a$10$abcdefghijklmnopqrstuv");
        return userAccountRepository.save(user);
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
        return restaurantRepository.save(restaurant);
    }

    private Order seedOrder(UserAccount customer, Restaurant restaurant, OrderStatus status) {
        Order order = new Order();
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
        return orderRepository.save(order);
    }
}
