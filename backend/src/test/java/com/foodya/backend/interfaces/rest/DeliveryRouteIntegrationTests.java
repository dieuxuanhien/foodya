package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.ports.out.*;
import com.foodya.backend.domain.entities.*;
import com.foodya.backend.domain.value_objects.*;
import com.foodya.backend.infrastructure.persistence.models.*;
import com.foodya.backend.infrastructure.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeliveryRouteIntegrationTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AdminUserRepository adminUserRepository;
    @Autowired
    private AdminRestaurantRepository adminRestaurantRepository;
    @Autowired
    private AdminMenuItemRepository adminMenuItemRepository;
    @Autowired
    private MenuCategoryPort menuCategoryPort;
    @Autowired
    private OrderManagementRepository orderManagementRepository;
    @Autowired
    private DriverOnlineSessionPort driverOnlineSessionPort;

    private String customerToken;
    private String driverToken;
    private UUID customerId;
    private UUID driverId;
    private UUID orderId;

    @BeforeEach
    void setup() throws Exception {
        // create customer
        String customerEmail = "route-customer-" + System.currentTimeMillis() + "@test.local";
        String signupJson = String.format("""
                {"fullName":"Route Customer","email":"%s","phoneNumber":"+84901234567","password":"Strong@123","role":"CUSTOMER"}
                """, customerEmail);
        String signupResp = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(signupJson))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode cData = objectMapper.readTree(signupResp).path("data");
        customerToken = cData.path("accessToken").asText();
        customerId = UUID.fromString(cData.path("userId").asText());

        // create driver
        String driverEmail = "route-driver-" + System.currentTimeMillis() + "@test.local";
        signupJson = String.format("""
                {"fullName":"Route Driver","email":"%s","phoneNumber":"+84901234568","password":"Strong@123","role":"DELIVERY"}
                """, driverEmail);
        signupResp = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(signupJson))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        TokenPair dTokens = objectMapper.readValue(objectMapper.readTree(signupResp).get("data").toString(), TokenPair.class);
        driverToken = dTokens.accessToken();
        driverId = UUID.fromString(objectMapper.readTree(signupResp).path("data").path("userId").asText());

        // go online
        mockMvc.perform(post("/api/v1/delivery/status/online")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk());

        // create restaurant + menu item
        UserAccountPersistenceModel owner = new UserAccountPersistenceModel();
        owner.setId(UUID.randomUUID());
        owner.setEmail("route-merchant@test.local");
        owner.setPasswordHash("hash");
        owner.setFullName("Route Merchant");
        owner.setRole(UserRole.MERCHANT);
        owner.setStatus(UserAccountStatus.ACTIVE);
        owner.setCreatedAt(OffsetDateTime.now());
        owner = adminUserRepository.save(owner);

        RestaurantPersistenceModel restaurant = new RestaurantPersistenceModel();
        restaurant.setId(UUID.randomUUID());
        restaurant.setOwnerId(owner.getId());
        restaurant.setName("Route Restaurant");
        restaurant.setPhone("+84901234569");
        restaurant.setAddress("123 Test St");
        restaurant.setLatitude(new BigDecimal("10.762622"));
        restaurant.setLongitude(new BigDecimal("106.660172"));
        restaurant.setH3IndexRes9("8929a4825ffffff");
        restaurant.setMaxDeliveryKm(new BigDecimal("5.0"));
        restaurant.setStatus(RestaurantStatus.APPROVED);
        restaurant.setCreatedAt(OffsetDateTime.now());
        restaurant = adminRestaurantRepository.save(restaurant);
        UUID restaurantId = restaurant.getId();

        MenuCategoryPersistenceModel cat = new MenuCategoryPersistenceModel();
        cat.setId(UUID.randomUUID());
        cat.setRestaurantId(restaurantId);
        cat.setName("Test Category");
        cat.setIsActive(true);
        cat.setCreatedAt(OffsetDateTime.now());
        cat = menuCategoryPort.save(cat).orElseThrow();

        MenuItemPersistenceModel item = new MenuItemPersistenceModel();
        item.setId(UUID.randomUUID());
        item.setRestaurantId(restaurantId);
        item.setCategoryId(cat.getId());
        item.setName("Test Dish");
        item.setPrice(new BigDecimal("50000"));
        item.setIsActive(true);
        item.setIsAvailable(true);
        item.setCreatedAt(OffsetDateTime.now());
        item = adminMenuItemRepository.save(item);

        // place order
        OrderPersistenceModel order = new OrderPersistenceModel();
        order.setId(UUID.randomUUID());
        order.setCustomerUserId(customerId);
        order.setRestaurantId(restaurantId);
        order.setOrderCode("ROUTE-" + System.currentTimeMillis());
        order.setStatus(OrderStatus.ASSIGNED);
        order.setDriverUserId(driverId);
        order.setPaymentMethod(PaymentMethod.COD);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setDeliveryAddress("456 Customer St");
        order.setDeliveryLatitude(new BigDecimal("10.772622"));
        order.setDeliveryLongitude(new BigDecimal("106.670172"));
        order.setDeliveryFee(new BigDecimal("15000"));
        order.setTotalAmount(new BigDecimal("65000"));
        order.setPlacedAt(OffsetDateTime.now());
        order.setCreatedAt(OffsetDateTime.now());
        order = orderManagementRepository.save(order);
        orderId = order.getId();

        // trigger route planning explicitly (the auto-assign in batching service normally does this)
        mockMvc.perform(post("/test-internal/orders/" + orderId + "/plan-route")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk());
    }

    @Test
    void customerCanFetchRouteForOwnOrder() throws Exception {
        mockMvc.perform(get("/api/v1/customer/orders/" + orderId + "/route")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.encodedPolyline").isNotEmpty())
                .andExpect(jsonPath("$.data.distanceMeters", greaterThan(0)))
                .andExpect(jsonPath("$.data.durationSeconds", greaterThan(0)))
                .andExpect(jsonPath("$.data.waypoints", hasSize(greaterThan(1))));
    }

    @Test
    void customerCannotFetchRouteForOtherUsersOrder() throws Exception {
        // create another customer
        String otherEmail = "other-customer-" + System.currentTimeMillis() + "@test.local";
        String signupJson = String.format("""
                {"fullName":"Other Customer","email":"%s","phoneNumber":"+84901234599","password":"Strong@123","role":"CUSTOMER"}
                """, otherEmail);
        String resp = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(signupJson))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String otherToken = objectMapper.readValue(objectMapper.readTree(resp).get("data").toString(), TokenPair.class).accessToken();

        mockMvc.perform(get("/api/v1/customer/orders/" + orderId + "/route")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void driverCanFetchRouteForAssignedOrder() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/orders/" + orderId + "/route")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.encodedPolyline").isNotEmpty())
                .andExpect(jsonPath("$.data.distanceMeters", greaterThan(0)));
    }

    @Test
    void driverCannotFetchRouteForUnassignedOrder() throws Exception {
        // create another driver
        String otherEmail = "other-driver-" + System.currentTimeMillis() + "@test.local";
        String signupJson = String.format("""
                {"fullName":"Other Driver","email":"%s","phoneNumber":"+84901234598","password":"Strong@123","role":"DELIVERY"}
                """, otherEmail);
        String resp = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(signupJson))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String otherToken = objectMapper.readValue(objectMapper.readTree(resp).get("data").toString(), TokenPair.class).accessToken();

        mockMvc.perform(get("/api/v1/delivery/orders/" + orderId + "/route")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }
}
