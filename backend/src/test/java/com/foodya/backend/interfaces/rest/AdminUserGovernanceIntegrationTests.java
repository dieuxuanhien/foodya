package com.foodya.backend.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.value_objects.PaymentMethod;
import com.foodya.backend.domain.value_objects.PaymentStatus;
import com.foodya.backend.domain.value_objects.RestaurantStatus;
import com.foodya.backend.domain.value_objects.UserRole;
import com.foodya.backend.domain.value_objects.UserStatus;
import com.foodya.backend.domain.entities.Order;
import com.foodya.backend.domain.entities.Restaurant;
import com.foodya.backend.domain.entities.UserAccount;
import com.foodya.backend.infrastructure.repository.OrderRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserGovernanceIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private OrderRepository orderRepository;

        @Autowired
        private CartItemRepository cartItemRepository;

        @Autowired
        private CartRepository cartRepository;

        @Autowired
        private MenuItemRepository menuItemRepository;

        @Autowired
        private MenuCategoryRepository menuCategoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanData() {
        orderRepository.deleteAll();
                cartItemRepository.deleteAll();
                cartRepository.deleteAll();
                menuItemRepository.deleteAll();
                menuCategoryRepository.deleteAll();
        restaurantRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void adminCanListLockUnlockAndDeleteUser() throws Exception {
        UserAccount admin = saveUser("admin_01", "admin1@foodya.test", "+84990000001", UserRole.ADMIN, "Admin@123");
        UserAccount customer = saveUser("customer_01", "customer1@foodya.test", "+84990000011", UserRole.CUSTOMER, "Cus@12345");

        String adminToken = login(admin.getUsername(), "Admin@123");

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("q", "customer_01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].username").value("customer_01"));

        mockMvc.perform(post("/api/v1/admin/users/{id}/lock", customer.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("LOCKED"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("""
                                {"usernameOrEmail":"customer_01","password":"Cus@12345"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/users/{id}/unlock", customer.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(delete("/api/v1/admin/users/{id}", customer.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteReturnsConflictWhenUserHasLinkedActiveOrders() throws Exception {
        UserAccount admin = saveUser("admin_02", "admin2@foodya.test", "+84990000002", UserRole.ADMIN, "Admin@123");
        UserAccount customer = saveUser("customer_02", "customer2@foodya.test", "+84990000012", UserRole.CUSTOMER, "Cus@12345");
        UserAccount merchant = saveUser("merchant_02", "merchant2@foodya.test", "+84990000022", UserRole.MERCHANT, "Mer@12345");

        Restaurant restaurant = new Restaurant();
        restaurant.setOwnerUserId(merchant.getId());
        restaurant.setName("Resto 02");
        restaurant.setCuisineType("Vietnamese");
        restaurant.setDescription("Test restaurant");
        restaurant.setAddressLine("123 Test Street");
        restaurant.setLatitude(new BigDecimal("10.7750000"));
        restaurant.setLongitude(new BigDecimal("106.7000000"));
        restaurant.setH3IndexRes9("8928308280fffff");
        restaurant.setStatus(RestaurantStatus.ACTIVE);
        restaurant.setOpen(true);
        restaurant.setMaxDeliveryKm(new BigDecimal("10.000"));
        restaurant = restaurantRepository.save(restaurant);

        Order order = new Order();
        order.setOrderCode("ODR-TEST-0001");
        order.setCustomerUserId(customer.getId());
        order.setIdempotencyKey("idem-test-0001");
        order.setRestaurantId(restaurant.getId());
        order.setStatus(OrderStatus.PENDING);
        order.setDeliveryAddress("456 Test Avenue");
        order.setDeliveryLatitude(new BigDecimal("10.7760000"));
        order.setDeliveryLongitude(new BigDecimal("106.7010000"));
        order.setCustomerNote("note");
        order.setSubtotalAmount(new BigDecimal("100000.00"));
        order.setDeliveryFee(new BigDecimal("15000.00"));
        order.setTotalAmount(new BigDecimal("115000.00"));
        order.setPaymentMethod(PaymentMethod.COD);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setCommissionAmount(new BigDecimal("10000.00"));
        order.setShippingFeeMarginAmount(new BigDecimal("0.00"));
        order.setPlatformProfitAmount(new BigDecimal("10000.00"));
        orderRepository.save(order);

        String adminToken = login(admin.getUsername(), "Admin@123");

        mockMvc.perform(delete("/api/v1/admin/users/{id}", customer.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    private UserAccount saveUser(String username,
                                 String email,
                                 String phone,
                                 UserRole role,
                                 String rawPassword) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setEmail(email);
        user.setPhoneNumber(phone);
        user.setFullName(username);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        return userAccountRepository.save(user);
    }

    private String login(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequestPayload(username, password));
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).path("data").path("accessToken").asText();
    }

        private record LoginRequestPayload(String usernameOrEmail, String password) {
    }
}
