package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.ports.out.GeoPort;
import com.foodya.backend.application.ports.out.TokenPort;
import com.foodya.backend.domain.value_objects.RestaurantStatus;
import com.foodya.backend.domain.value_objects.UserRole;
import com.foodya.backend.infrastructure.mapper.AuthPersistenceMapper;
import com.foodya.backend.domain.value_objects.UserStatus;
import com.foodya.backend.domain.entities.MenuCategory;
import com.foodya.backend.domain.entities.MenuItem;
import com.foodya.backend.domain.entities.Restaurant;
import com.foodya.backend.domain.entities.UserAccount;
import com.foodya.backend.infrastructure.repository.AiChatHistoryRepository;
import com.foodya.backend.infrastructure.repository.CartItemRepository;
import com.foodya.backend.infrastructure.repository.CartRepository;
import com.foodya.backend.infrastructure.repository.MenuCategoryRepository;
import com.foodya.backend.infrastructure.repository.MenuItemRepository;
import com.foodya.backend.infrastructure.repository.OrderManagementRepository;
import com.foodya.backend.infrastructure.repository.OrderRepository;
import com.foodya.backend.infrastructure.repository.RestaurantRepository;
import com.foodya.backend.infrastructure.repository.UserAccountRepository;
import com.foodya.backend.infrastructure.mapper.MenuCategoryMapper;
import com.foodya.backend.infrastructure.mapper.MenuItemMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerAiChatIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private MenuCategoryRepository menuCategoryRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private AiChatHistoryRepository aiChatHistoryRepository;

    @Autowired
    private OrderManagementRepository orderManagementRepository;

    @Autowired
    private OrderRepository orderRepository;

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

    @Autowired
    private MenuCategoryMapper menuCategoryMapper;

    @Autowired
    private MenuItemMapper menuItemMapper;

    @BeforeEach
    void setUp() {
        aiChatHistoryRepository.deleteAll();
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
    void createChatReturnsInternalAvailableRecommendations() throws Exception {
        UserAccount customer = seedUser("ai-customer-1", UserRole.CUSTOMER);
        UserAccount merchant = seedUser("ai-merchant-1", UserRole.MERCHANT);
        Restaurant restaurant = seedRestaurant(merchant, "Pho Chef");
        MenuCategory category = seedCategory(restaurant, "Main", 1);
        seedMenuItem(restaurant, category, "Pho Bo", new BigDecimal("65000"), true);
        seedMenuItem(restaurant, category, "Pho Ga", new BigDecimal("62000"), true);
        seedMenuItem(restaurant, category, "Hidden Pho", new BigDecimal("50000"), false);

        String token = tokenService.issueAccessToken(AuthPersistenceMapper.toModel(customer), UUID.randomUUID().toString());

        mockMvc.perform(post("/api/v1/customer/ai/chats")
                        .header("Authorization", "Bearer " + token)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("""
                                {
                                  "prompt":"pho for lunch",
                                  "lat":10.77,
                                  "lng":106.70
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.prompt").value("pho for lunch"))
                .andExpect(jsonPath("$.data.recommendations[0].menuItemName").exists())
                .andExpect(jsonPath("$.data.recommendations[*].menuItemName")
                    .value(Objects.requireNonNull(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("Hidden Pho")))));
    }

    @Test
    void historyReturnsNewestFirst() throws Exception {
        UserAccount customer = seedUser("ai-customer-2", UserRole.CUSTOMER);
        UserAccount merchant = seedUser("ai-merchant-2", UserRole.MERCHANT);
        Restaurant restaurant = seedRestaurant(merchant, "Com Kitchen");
        MenuCategory category = seedCategory(restaurant, "Main", 1);
        seedMenuItem(restaurant, category, "Com Tam", new BigDecimal("45000"), true);

        String token = tokenService.issueAccessToken(AuthPersistenceMapper.toModel(customer), UUID.randomUUID().toString());

        mockMvc.perform(post("/api/v1/customer/ai/chats")
                        .header("Authorization", "Bearer " + token)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("{" +
                                "\"prompt\":\"first request\"" +
                                "}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/customer/ai/chats")
                        .header("Authorization", "Bearer " + token)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("{" +
                                "\"prompt\":\"second request\"" +
                                "}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/customer/ai/chats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].prompt").value("second request"))
                .andExpect(jsonPath("$.data[1].prompt").value("first request"));
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
        var persistenceModel = restaurantMapper.toPersistence(restaurant);
        @SuppressWarnings("null")
        var saved = restaurantRepository.save(persistenceModel);
        return restaurantMapper.toDomain(saved);
    }

    private MenuCategory seedCategory(Restaurant restaurant, String name, int sortOrder) {
        MenuCategory category = new MenuCategory();
        category.setRestaurantId(restaurant.getId());
        category.setName(name);
        category.setSortOrder(sortOrder);
        category.setActive(true);
        var persistenceModel = menuCategoryMapper.toPersistence(category);
        @SuppressWarnings("null")
        var saved = menuCategoryRepository.save(persistenceModel);
        return menuCategoryMapper.toDomain(saved);
    }

    private MenuItem seedMenuItem(Restaurant restaurant,
                                  MenuCategory category,
                                  String name,
                                  BigDecimal price,
                                  boolean available) {
        MenuItem item = new MenuItem();
        item.setRestaurantId(restaurant.getId());
        item.setCategoryId(category.getId());
        item.setName(name);
        item.setDescription(name + " description");
        item.setPrice(price);
        item.setActive(true);
        item.setAvailable(available);
        var persistenceModel = menuItemMapper.toPersistence(item);
        @SuppressWarnings("null")
        var saved = menuItemRepository.save(persistenceModel);
        return menuItemMapper.toDomain(saved);
    }
}
