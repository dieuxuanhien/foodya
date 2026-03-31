package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.ports.out.GeoPort;
import com.foodya.backend.application.ports.out.TokenPort;
import com.foodya.backend.domain.value_objects.RestaurantStatus;
import com.foodya.backend.infrastructure.adapter.mapper.AuthPersistenceMapper;
import com.foodya.backend.domain.value_objects.UserRole;
import com.foodya.backend.domain.value_objects.UserStatus;
import com.foodya.backend.domain.entities.MenuCategory;
import com.foodya.backend.domain.entities.MenuItem;
import com.foodya.backend.domain.entities.Restaurant;
import com.foodya.backend.domain.entities.UserAccount;
import com.foodya.backend.infrastructure.repository.CartItemRepository;
import com.foodya.backend.infrastructure.repository.CartRepository;
import com.foodya.backend.infrastructure.repository.MenuCategoryRepository;
import com.foodya.backend.infrastructure.repository.MenuItemRepository;
import com.foodya.backend.infrastructure.repository.OrderRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerCartIntegrationTests {

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
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TokenPort tokenService;

    @Autowired
    private GeoPort geoService;

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
    void addUpdateRemoveAndClearCartLifecycle() throws Exception {
        String customerToken = customerAccessToken("cart-customer", "cart-customer@example.com", "+84909000001");

        Restaurant restaurant = seedRestaurant("Pho Cart", new BigDecimal("10.7770000"), new BigDecimal("106.7000000"));
        MenuCategory category = seedCategory(restaurant, "Main", 1);
        MenuItem menuItem = seedMenuItem(restaurant, category, "Pho Bo", new BigDecimal("60000"));

        mockMvc.perform(get("/api/v1/customer/carts/active")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cartId").isEmpty())
                .andExpect(jsonPath("$.data.itemCount").value(0));

        mockMvc.perform(post("/api/v1/customer/carts/active/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("{" +
                                "\"menuItemId\":\"" + menuItem.getId() + "\"," +
                                "\"quantity\":2," +
                                "\"note\":\"less spicy\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.restaurantId").value(restaurant.getId().toString()))
                .andExpect(jsonPath("$.data.itemCount").value(2))
                .andExpect(jsonPath("$.data.subtotal").value(120000));

        mockMvc.perform(patch("/api/v1/customer/carts/active/items/{menuItemId}", menuItem.getId())
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("{\"quantity\":1,\"note\":\"normal\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCount").value(1))
                .andExpect(jsonPath("$.data.subtotal").value(60000));

        mockMvc.perform(delete("/api/v1/customer/carts/active/items/{menuItemId}", menuItem.getId())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCount").value(0));

        mockMvc.perform(delete("/api/v1/customer/carts/active/items")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCount").value(0));
    }

    @Test
    void rejectDifferentRestaurantInSameActiveCart() throws Exception {
        String customerToken = customerAccessToken("cart-scope", "cart-scope@example.com", "+84909000002");

        Restaurant first = seedRestaurant("First", new BigDecimal("10.7770000"), new BigDecimal("106.7000000"));
        Restaurant second = seedRestaurant("Second", new BigDecimal("10.7800000"), new BigDecimal("106.7100000"));
        MenuCategory firstCategory = seedCategory(first, "Main", 1);
        MenuCategory secondCategory = seedCategory(second, "Main", 1);
        MenuItem firstItem = seedMenuItem(first, firstCategory, "Bun Cha", new BigDecimal("50000"));
        MenuItem secondItem = seedMenuItem(second, secondCategory, "Com Tam", new BigDecimal("45000"));

        mockMvc.perform(post("/api/v1/customer/carts/active/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("{\"menuItemId\":\"" + firstItem.getId() + "\",\"quantity\":1}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/customer/carts/active/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("{\"menuItemId\":\"" + secondItem.getId() + "\",\"quantity\":1}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private String customerAccessToken(String username, String email, String phoneNumber) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setFullName(username);
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash("$2a$10$abcdefghijklmnopqrstuv");
        UserAccount saved = userAccountRepository.save(user);
        return tokenService.issueAccessToken(AuthPersistenceMapper.toModel(saved), UUID.randomUUID().toString());
    }

    private Restaurant seedRestaurant(String name, BigDecimal lat, BigDecimal lng) {
        UserAccount owner = new UserAccount();
        owner.setUsername("owner-" + name.toLowerCase());
        owner.setEmail(name.toLowerCase() + "@merchant.test");
        owner.setPhoneNumber("+8490" + Math.abs(name.hashCode() % 10000000));
        owner.setFullName(name + " Owner");
        owner.setRole(UserRole.MERCHANT);
        owner.setStatus(UserStatus.ACTIVE);
        owner.setPasswordHash("$2a$10$abcdefghijklmnopqrstuv");
        UserAccount savedOwner = userAccountRepository.save(owner);

        Restaurant restaurant = new Restaurant();
        restaurant.setOwnerUserId(savedOwner.getId());
        restaurant.setName(name);
        restaurant.setCuisineType("Vietnamese");
        restaurant.setDescription(name + " description");
        restaurant.setAddressLine("123 Test Street");
        restaurant.setLatitude(lat);
        restaurant.setLongitude(lng);
        restaurant.setH3IndexRes9(geoService.h3Res9(lat.doubleValue(), lng.doubleValue()));
        restaurant.setStatus(RestaurantStatus.ACTIVE);
        restaurant.setOpen(true);
        restaurant.setMaxDeliveryKm(new BigDecimal("5.0"));
        return restaurantRepository.save(restaurant);
    }

    private MenuCategory seedCategory(Restaurant restaurant, String name, int sortOrder) {
        MenuCategory category = new MenuCategory();
        category.setRestaurantId(restaurant.getId());
        category.setName(name);
        category.setSortOrder(sortOrder);
        category.setActive(true);
        return menuCategoryRepository.save(category);
    }

    private MenuItem seedMenuItem(Restaurant restaurant, MenuCategory category, String name, BigDecimal price) {
        MenuItem item = new MenuItem();
        item.setRestaurantId(restaurant.getId());
        item.setCategoryId(category.getId());
        item.setName(name);
        item.setDescription(name + " description");
        item.setPrice(price);
        item.setActive(true);
        item.setAvailable(true);
        return menuItemRepository.save(item);
    }
}
