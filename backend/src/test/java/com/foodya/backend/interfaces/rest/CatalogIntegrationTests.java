package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.service.GeoService;
import com.foodya.backend.domain.persistence.MenuCategory;
import com.foodya.backend.domain.persistence.MenuItem;
import com.foodya.backend.domain.persistence.Restaurant;
import com.foodya.backend.domain.model.RestaurantStatus;
import com.foodya.backend.domain.persistence.UserAccount;
import com.foodya.backend.domain.model.UserRole;
import com.foodya.backend.domain.model.UserStatus;
import com.foodya.backend.infrastructure.repository.MenuCategoryRepository;
import com.foodya.backend.infrastructure.repository.CartItemRepository;
import com.foodya.backend.infrastructure.repository.CartRepository;
import com.foodya.backend.infrastructure.repository.MenuItemRepository;
import com.foodya.backend.infrastructure.repository.OrderRepository;
import com.foodya.backend.infrastructure.repository.RestaurantRepository;
import com.foodya.backend.infrastructure.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogIntegrationTests {

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
    private GeoService geoService;

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
    void searchReturnsGroupedRestaurantsWithMatchedItems() throws Exception {
        Restaurant restaurant = seedRestaurant("Pho House", new BigDecimal("10.7770000"), new BigDecimal("106.7000000"));
        MenuCategory category = seedCategory(restaurant, "Main", 1);
        seedMenuItem(restaurant, category, "Pho Tai", new BigDecimal("55000"));

        mockMvc.perform(get("/api/v1/restaurants")
                        .param("q", "Pho")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].restaurantName").value("Pho House"))
                .andExpect(jsonPath("$.data[0].matchedItems[0].name").value("Pho Tai"))
                .andExpect(jsonPath("$.meta.page").value(0));
    }

    @Test
    void nearbySortsByDistanceAscending() throws Exception {
        seedRestaurant("Near A", new BigDecimal("10.7771000"), new BigDecimal("106.7001000"));
        seedRestaurant("Near B", new BigDecimal("10.7900000"), new BigDecimal("106.7200000"));

        mockMvc.perform(get("/api/v1/restaurants/nearby")
                        .param("lat", "10.7770000")
                        .param("lng", "106.7000000")
                        .param("radiusKm", "10")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].restaurantName").value("Near A"));
    }

    @Test
    void searchRejectsInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/v1/restaurants")
                        .param("size", "999"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

        @Test
        void menuItemsSupportsKeywordCategoryAndSortValidation() throws Exception {
        Restaurant restaurant = seedRestaurant("Menu Search", new BigDecimal("10.7770000"), new BigDecimal("106.7000000"));
        MenuCategory noodle = seedCategory(restaurant, "Noodle", 1);
        MenuCategory rice = seedCategory(restaurant, "Rice", 2);
        seedMenuItem(restaurant, noodle, "Pho Bo", new BigDecimal("60000"));
        seedMenuItem(restaurant, rice, "Com Tam", new BigDecimal("45000"));

        mockMvc.perform(get("/api/v1/restaurants/{id}/menu-items", restaurant.getId())
                .param("q", "pho")
                .param("categoryId", noodle.getId().toString())
                .param("sort", "price_desc")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].name").value("Pho Bo"));

        mockMvc.perform(get("/api/v1/restaurants/{id}/menu-items", restaurant.getId())
                .param("sort", "unsupported_sort"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }

    private Restaurant seedRestaurant(String name, BigDecimal lat, BigDecimal lng) {
        UserAccount owner = new UserAccount();
        owner.setUsername("owner-" + name.replace(" ", "-").toLowerCase());
        owner.setEmail(name.replace(" ", "").toLowerCase() + "@example.com");
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
