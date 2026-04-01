package com.foodya.backend.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.value_objects.PaymentMethod;
import com.foodya.backend.domain.value_objects.PaymentStatus;
import com.foodya.backend.domain.value_objects.RestaurantStatus;
import com.foodya.backend.domain.value_objects.UserRole;
import com.foodya.backend.domain.value_objects.UserStatus;
import com.foodya.backend.domain.entities.MenuCategory;
import com.foodya.backend.domain.entities.MenuItem;
import com.foodya.backend.domain.entities.Order;
import com.foodya.backend.domain.entities.Restaurant;
import com.foodya.backend.domain.entities.UserAccount;
import com.foodya.backend.infrastructure.repository.MenuCategoryRepository;
import com.foodya.backend.infrastructure.repository.MenuItemRepository;
import com.foodya.backend.infrastructure.repository.OrderItemRepository;
import com.foodya.backend.infrastructure.repository.OrderRepository;
import com.foodya.backend.infrastructure.repository.RestaurantRepository;
import com.foodya.backend.infrastructure.repository.UserAccountRepository;
import com.foodya.backend.infrastructure.repository.CartItemRepository;
import com.foodya.backend.infrastructure.repository.CartRepository;
import com.foodya.backend.infrastructure.mapper.MenuCategoryMapper;
import com.foodya.backend.infrastructure.mapper.MenuItemMapper;
import com.foodya.backend.infrastructure.mapper.OrderMapper;
import com.foodya.backend.infrastructure.mapper.RestaurantMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Objects;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminGovernanceIntegrationTests {

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
    private MenuCategoryRepository menuCategoryRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RestaurantMapper restaurantMapper;

    @Autowired
    private MenuCategoryMapper menuCategoryMapper;

    @Autowired
    private MenuItemMapper menuItemMapper;

    @BeforeEach
    void cleanData() {
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        menuItemRepository.deleteAll();
        menuCategoryRepository.deleteAll();
        restaurantRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void adminCanApproveRejectAndDeleteRestaurant() throws Exception {
        UserAccount admin = saveUser("admin_gov_01", "admin_gov_01@foodya.test", "+84990100001", UserRole.ADMIN, "Admin@123");
        UserAccount merchant = saveUser("merchant_gov_01", "merchant_gov_01@foodya.test", "+84990100011", UserRole.MERCHANT, "Mer@12345");

        Restaurant restaurant = saveRestaurant(merchant, RestaurantStatus.PENDING, "Governance One");
        String adminToken = login(admin.getUsername(), "Admin@123");

        mockMvc.perform(post("/api/v1/admin/restaurants/{id}/approve", restaurant.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(post("/api/v1/admin/restaurants/{id}/reject", restaurant.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        mockMvc.perform(delete("/api/v1/admin/restaurants/{id}", restaurant.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteRestaurantReturnsConflictWhenActiveOrdersExist() throws Exception {
        UserAccount admin = saveUser("admin_gov_02", "admin_gov_02@foodya.test", "+84990100002", UserRole.ADMIN, "Admin@123");
        UserAccount merchant = saveUser("merchant_gov_02", "merchant_gov_02@foodya.test", "+84990100012", UserRole.MERCHANT, "Mer@12345");
        UserAccount customer = saveUser("customer_gov_02", "customer_gov_02@foodya.test", "+84990100022", UserRole.CUSTOMER, "Cus@12345");

        Restaurant restaurant = saveRestaurant(merchant, RestaurantStatus.ACTIVE, "Governance Two");
        saveOrder(customer, restaurant, OrderStatus.PENDING, "ODR-GOV-0201");

        String adminToken = login(admin.getUsername(), "Admin@123");

        mockMvc.perform(delete("/api/v1/admin/restaurants/{id}", restaurant.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void adminCanGovernOrdersAndHardDeleteMenuItem() throws Exception {
        UserAccount admin = saveUser("admin_gov_03", "admin_gov_03@foodya.test", "+84990100003", UserRole.ADMIN, "Admin@123");
        UserAccount merchant = saveUser("merchant_gov_03", "merchant_gov_03@foodya.test", "+84990100013", UserRole.MERCHANT, "Mer@12345");
        UserAccount customer = saveUser("customer_gov_03", "customer_gov_03@foodya.test", "+84990100023", UserRole.CUSTOMER, "Cus@12345");

        Restaurant restaurant = saveRestaurant(merchant, RestaurantStatus.ACTIVE, "Governance Three");
        MenuCategory category = saveCategory(restaurant, "Main");
        MenuItem menuItem = saveMenuItem(restaurant, category, "Pho");
        Order order = saveOrder(customer, restaurant, OrderStatus.PENDING, "ODR-GOV-0301");

        String adminToken = login(admin.getUsername(), "Admin@123");

        mockMvc.perform(get("/api/v1/admin/orders")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].orderId").value(order.getId().toString()));

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", order.getId())
                        .header("Authorization", "Bearer " + adminToken)
            .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("""
                                {"status":"ACCEPTED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", order.getId())
                .header("Authorization", "Bearer " + adminToken)
            .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content("""
                    {"status":"ASSIGNED"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("ASSIGNED"));

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", order.getId())
                .header("Authorization", "Bearer " + adminToken)
            .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content("""
                    {"status":"DELIVERING"}
                    """))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", order.getId())
                .header("Authorization", "Bearer " + adminToken)
            .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content("""
                    {"status":"PREPARING"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PREPARING"));

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", order.getId())
                .header("Authorization", "Bearer " + adminToken)
            .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content("""
                    {"status":"DELIVERING"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("DELIVERING"));

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", order.getId())
                        .header("Authorization", "Bearer " + adminToken)
            .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("""
                                {"status":"SUCCESS"}
                                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SUCCESS"));

        mockMvc.perform(delete("/api/v1/admin/orders/{id}", order.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/admin/menu-items/{id}", menuItem.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
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

    private Restaurant saveRestaurant(UserAccount merchant, RestaurantStatus status, String name) {
        Restaurant restaurant = new Restaurant();
        restaurant.setOwnerUserId(merchant.getId());
        restaurant.setName(name);
        restaurant.setCuisineType("Vietnamese");
        restaurant.setDescription("test");
        restaurant.setAddressLine("123 Test Street");
        restaurant.setLatitude(new BigDecimal("10.7750000"));
        restaurant.setLongitude(new BigDecimal("106.7000000"));
        restaurant.setH3IndexRes9("8928308280fffff");
        restaurant.setStatus(status);
        restaurant.setOpen(true);
        restaurant.setMaxDeliveryKm(new BigDecimal("10.000"));
        var persistenceModel = restaurantMapper.toPersistence(restaurant);
        @SuppressWarnings("null")
        var saved = restaurantRepository.save(persistenceModel);
        return restaurantMapper.toDomain(saved);
    }

    private Order saveOrder(UserAccount customer, Restaurant restaurant, OrderStatus status, String orderCode) {
        Order order = new Order();
        order.setOrderCode(orderCode);
        order.setCustomerUserId(customer.getId());
        order.setIdempotencyKey("idem-" + orderCode);
        order.setRestaurantId(restaurant.getId());
        order.setStatus(status);
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
        var persistenceModel = orderMapper.toPersistence(order);
        @SuppressWarnings("null")
        var saved = orderRepository.save(persistenceModel);
        return orderMapper.toDomain(saved);
    }

    private MenuCategory saveCategory(Restaurant restaurant, String name) {
        MenuCategory category = new MenuCategory();
        category.setRestaurantId(restaurant.getId());
        category.setName(name);
        category.setSortOrder(1);
        category.setActive(true);
        var persistenceModel = menuCategoryMapper.toPersistence(category);
        @SuppressWarnings("null")
        var saved = menuCategoryRepository.save(persistenceModel);
        return menuCategoryMapper.toDomain(saved);
    }

    private MenuItem saveMenuItem(Restaurant restaurant, MenuCategory category, String name) {
        MenuItem menuItem = new MenuItem();
        menuItem.setRestaurantId(restaurant.getId());
        menuItem.setCategoryId(category.getId());
        menuItem.setName(name);
        menuItem.setDescription("test");
        menuItem.setPrice(new BigDecimal("50000.00"));
        menuItem.setActive(true);
        menuItem.setAvailable(true);
        var persistenceModel = menuItemMapper.toPersistence(menuItem);
        @SuppressWarnings("null")
        var saved = menuItemRepository.save(persistenceModel);
        return menuItemMapper.toDomain(saved);
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
