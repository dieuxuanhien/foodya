package com.foodya.backend.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodya.backend.application.ports.out.TokenPort;
import com.foodya.backend.infrastructure.mapper.AuthPersistenceMapper;
import com.foodya.backend.infrastructure.repository.CartItemRepository;
import com.foodya.backend.infrastructure.repository.CartRepository;
import com.foodya.backend.infrastructure.repository.MenuCategoryRepository;
import com.foodya.backend.infrastructure.repository.MenuItemRepository;
import com.foodya.backend.infrastructure.repository.OrderRepository;
import com.foodya.backend.infrastructure.repository.RestaurantRepository;
import com.foodya.backend.domain.entities.UserAccount;
import com.foodya.backend.domain.value_objects.UserRole;
import com.foodya.backend.domain.value_objects.UserStatus;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MerchantCatalogIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

        @Autowired
        private RestaurantRepository restaurantRepository;

        @Autowired
        private MenuCategoryRepository menuCategoryRepository;

        @Autowired
        private MenuItemRepository menuItemRepository;

        @Autowired
        private CartItemRepository cartItemRepository;

        @Autowired
        private CartRepository cartRepository;

        @Autowired
        private OrderRepository orderRepository;

    @Autowired
    private TokenPort tokenService;

    @BeforeEach
    void setUp() {
                orderRepository.deleteAll();
                cartItemRepository.deleteAll();
                cartRepository.deleteAll();
                menuItemRepository.deleteAll();
                menuCategoryRepository.deleteAll();
                restaurantRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void merchantCanManageRestaurantCategoryAndMenuItemLifecycle() throws Exception {
        String merchantToken = merchantAccessToken("merchant-a", "merchant-a@example.com", "+84900111111");

        String createRestaurantBody = """
                {
                  "name":"Bun Bo Hub",
                  "cuisineType":"Vietnamese",
                  "description":"Hue style",
                  "addressLine":"1 Nguyen Hue",
                  "latitude":10.7771,
                  "longitude":106.7001,
                  "maxDeliveryKm":5,
                  "isOpen":true
                }
                """;

        String restaurantResponse = mockMvc.perform(post("/api/v1/merchant/restaurants")
                        .header("Authorization", "Bearer " + merchantToken)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(createRestaurantBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Bun Bo Hub"))
                .andReturn().getResponse().getContentAsString();

        String restaurantId = objectMapper.readTree(restaurantResponse).path("data").path("id").asText();

        String categoryResponse = mockMvc.perform(post("/api/v1/merchant/restaurants/{id}/menu-categories", restaurantId)
                        .header("Authorization", "Bearer " + merchantToken)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("{" +
                                "\"name\":\"Noodle\"," +
                                "\"sortOrder\":1," +
                                "\"isActive\":true}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String categoryId = objectMapper.readTree(categoryResponse).path("data").path("id").asText();

        String menuItemResponse = mockMvc.perform(post("/api/v1/merchant/restaurants/{id}/menu-items", restaurantId)
                        .header("Authorization", "Bearer " + merchantToken)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("{" +
                                "\"categoryId\":\"" + categoryId + "\"," +
                                "\"name\":\"Bun Bo\"," +
                                "\"description\":\"Spicy\"," +
                                "\"price\":65000," +
                                "\"isActive\":true," +
                                "\"isAvailable\":true}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String menuItemId = objectMapper.readTree(menuItemResponse).path("data").path("id").asText();

        mockMvc.perform(patch("/api/v1/merchant/menu-items/{id}/availability", menuItemId)
                        .header("Authorization", "Bearer " + merchantToken)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("{\"isAvailable\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false));

        mockMvc.perform(delete("/api/v1/merchant/menu-items/{id}", menuItemId)
                        .header("Authorization", "Bearer " + merchantToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void merchantCannotUpdateAnotherMerchantRestaurant() throws Exception {
        String ownerToken = merchantAccessToken("merchant-owner", "owner@example.com", "+84900111112");
        String attackerToken = merchantAccessToken("merchant-attacker", "attacker@example.com", "+84900111113");

        String restaurantResponse = mockMvc.perform(post("/api/v1/merchant/restaurants")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("""
                                {
                                  "name":"Secret Kitchen",
                                  "cuisineType":"Vietnamese",
                                  "description":"Owner only",
                                  "addressLine":"2 Le Loi",
                                  "latitude":10.7771,
                                  "longitude":106.7001,
                                  "maxDeliveryKm":5,
                                  "isOpen":true
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String restaurantId = objectMapper.readTree(restaurantResponse).path("data").path("id").asText();

        mockMvc.perform(patch("/api/v1/merchant/restaurants/{id}", restaurantId)
                        .header("Authorization", "Bearer " + attackerToken)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("""
                                {
                                  "name":"Hijacked",
                                  "cuisineType":"Vietnamese",
                                  "description":"invalid",
                                  "addressLine":"3 Le Loi",
                                  "latitude":10.7771,
                                  "longitude":106.7001,
                                  "maxDeliveryKm":5,
                                  "isOpen":false
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void merchantMenuItemPriceMustRespectConfiguredBounds() throws Exception {
        String merchantToken = merchantAccessToken("merchant-b", "merchant-b@example.com", "+84900111114");

        String restaurantResponse = mockMvc.perform(post("/api/v1/merchant/restaurants")
                        .header("Authorization", "Bearer " + merchantToken)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("""
                                {
                                  "name":"Price Bound Kitchen",
                                  "cuisineType":"Vietnamese",
                                  "description":"Bounds",
                                  "addressLine":"4 Le Loi",
                                  "latitude":10.7771,
                                  "longitude":106.7001,
                                  "maxDeliveryKm":5,
                                  "isOpen":true
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String restaurantId = objectMapper.readTree(restaurantResponse).path("data").path("id").asText();

        String categoryResponse = mockMvc.perform(post("/api/v1/merchant/restaurants/{id}/menu-categories", restaurantId)
                        .header("Authorization", "Bearer " + merchantToken)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("{\"name\":\"Noodle\",\"sortOrder\":1,\"isActive\":true}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String categoryId = objectMapper.readTree(categoryResponse).path("data").path("id").asText();

        mockMvc.perform(post("/api/v1/merchant/restaurants/{id}/menu-items", restaurantId)
                        .header("Authorization", "Bearer " + merchantToken)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("{" +
                                "\"categoryId\":\"" + categoryId + "\"," +
                                "\"name\":\"Invalid Cheap Item\"," +
                                "\"description\":\"Cheap\"," +
                                "\"price\":500," +
                                "\"isActive\":true," +
                                "\"isAvailable\":true}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private String merchantAccessToken(String username, String email, String phoneNumber) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setFullName(username);
        user.setRole(UserRole.MERCHANT);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash("$2a$10$abcdefghijklmnopqrstuv");
        UserAccount saved = userAccountRepository.save(user);
        return tokenService.issueAccessToken(AuthPersistenceMapper.toModel(saved), UUID.randomUUID().toString());
    }
}
