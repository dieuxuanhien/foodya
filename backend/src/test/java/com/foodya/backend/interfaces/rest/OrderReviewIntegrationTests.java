package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.ports.out.GeoPort;
import com.foodya.backend.application.ports.out.TokenPort;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.infrastructure.adapter.mapper.AuthPersistenceMapper;
import com.foodya.backend.domain.value_objects.PaymentMethod;
import com.foodya.backend.domain.value_objects.PaymentStatus;
import com.foodya.backend.domain.value_objects.RestaurantStatus;
import com.foodya.backend.domain.value_objects.UserRole;
import com.foodya.backend.domain.value_objects.UserStatus;
import com.foodya.backend.domain.entities.Order;
import com.foodya.backend.domain.entities.OrderReview;
import com.foodya.backend.domain.entities.Restaurant;
import com.foodya.backend.domain.entities.UserAccount;
import com.foodya.backend.infrastructure.repository.OrderManagementRepository;
import com.foodya.backend.infrastructure.repository.OrderRepository;
import com.foodya.backend.infrastructure.repository.OrderReviewRepository;
import com.foodya.backend.infrastructure.repository.CartItemRepository;
import com.foodya.backend.infrastructure.repository.CartRepository;
import com.foodya.backend.infrastructure.repository.MenuCategoryRepository;
import com.foodya.backend.infrastructure.repository.MenuItemRepository;
import com.foodya.backend.infrastructure.repository.RestaurantRepository;
import com.foodya.backend.infrastructure.repository.UserAccountRepository;
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
class OrderReviewIntegrationTests {

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
    private OrderReviewRepository orderReviewRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private TokenPort tokenService;

    @Autowired
    private GeoPort geoService;

    @BeforeEach
    void setUp() {
        orderReviewRepository.deleteAll();
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
    void customerCanCreateReviewAndRestaurantCanListReviews() throws Exception {
        UserAccount customer = seedUser("customer-r1", UserRole.CUSTOMER);
        UserAccount merchant = seedUser("merchant-r1", UserRole.MERCHANT);
        Restaurant restaurant = seedRestaurant(merchant, "Review Store");
        Order order = seedOrder(customer, restaurant, OrderStatus.SUCCESS);

        String customerToken = tokenService.issueAccessToken(AuthPersistenceMapper.toModel(customer), UUID.randomUUID().toString());

        mockMvc.perform(post("/api/v1/customer/orders/{id}/review", order.getId())
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("{\"stars\":5,\"comment\":\"Great meal\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(order.getId().toString()))
                .andExpect(jsonPath("$.data.stars").value(5));

        mockMvc.perform(get("/api/v1/restaurants/{id}/reviews", restaurant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].comment").value("Great meal"));
    }

    @Test
    void merchantCanRespondToOwnedReview() throws Exception {
        UserAccount customer = seedUser("customer-r2", UserRole.CUSTOMER);
        UserAccount merchant = seedUser("merchant-r2", UserRole.MERCHANT);
        Restaurant restaurant = seedRestaurant(merchant, "Response Store");
        Order order = seedOrder(customer, restaurant, OrderStatus.SUCCESS);

        String customerToken = tokenService.issueAccessToken(AuthPersistenceMapper.toModel(customer), UUID.randomUUID().toString());
        String merchantToken = tokenService.issueAccessToken(AuthPersistenceMapper.toModel(merchant), UUID.randomUUID().toString());

        mockMvc.perform(post("/api/v1/customer/orders/{id}/review", order.getId())
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("{\"stars\":4,\"comment\":\"Nice\"}"))
                .andExpect(status().isOk());

        OrderReview review = orderReviewRepository.findByOrderId(order.getId()).orElseThrow();

        mockMvc.perform(patch("/api/v1/merchant/reviews/{id}/response", review.getId())
                        .header("Authorization", "Bearer " + merchantToken)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("{\"response\":\"Thank you\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.merchantResponse").value("Thank you"));
    }

    @Test
    void customerCannotReviewNonSuccessOrder() throws Exception {
        UserAccount customer = seedUser("customer-r3", UserRole.CUSTOMER);
        UserAccount merchant = seedUser("merchant-r3", UserRole.MERCHANT);
        Restaurant restaurant = seedRestaurant(merchant, "Blocked Store");
        Order order = seedOrder(customer, restaurant, OrderStatus.DELIVERING);

        String customerToken = tokenService.issueAccessToken(AuthPersistenceMapper.toModel(customer), UUID.randomUUID().toString());

        mockMvc.perform(post("/api/v1/customer/orders/{id}/review", order.getId())
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("{\"stars\":5,\"comment\":\"Soon\"}"))
                .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.message").value("review is allowed only for SUCCESS orders"));
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
