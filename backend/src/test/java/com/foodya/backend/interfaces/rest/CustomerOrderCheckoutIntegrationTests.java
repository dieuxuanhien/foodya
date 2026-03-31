package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.port.out.RouteDistancePort;
import com.foodya.backend.application.service.GeoService;
import com.foodya.backend.application.service.TokenService;
import com.foodya.backend.domain.model.RestaurantStatus;
import com.foodya.backend.domain.model.UserRole;
import com.foodya.backend.domain.model.UserStatus;
import com.foodya.backend.domain.persistence.MenuCategory;
import com.foodya.backend.domain.persistence.MenuItem;
import com.foodya.backend.domain.persistence.Restaurant;
import com.foodya.backend.domain.persistence.UserAccount;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerOrderCheckoutIntegrationTests {

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
    private OrderRepository orderRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private GeoService geoService;

    @MockBean
    private RouteDistancePort routeDistancePort;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        orderRepository.deleteAll();
        menuItemRepository.deleteAll();
        menuCategoryRepository.deleteAll();
        restaurantRepository.deleteAll();
        userAccountRepository.deleteAll();

        given(routeDistancePort.routeDistanceKm(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .willReturn(new BigDecimal("3.000"));
    }

    @Test
    void createOrderAndRespectIdempotencyKey() throws Exception {
        String customerToken = customerAccessToken("checkout-customer", "checkout@example.com", "+84907000001");
        Restaurant restaurant = seedRestaurant("Order Place", new BigDecimal("10.7770000"), new BigDecimal("106.7000000"));
        MenuCategory category = seedCategory(restaurant, "Main", 1);
        MenuItem first = seedMenuItem(restaurant, category, "Pho", new BigDecimal("50000"));
        MenuItem second = seedMenuItem(restaurant, category, "Tea", new BigDecimal("10000"));

        String body = "{" +
                "\"restaurantId\":\"" + restaurant.getId() + "\"," +
                "\"items\":[{" +
                "\"menuItemId\":\"" + first.getId() + "\",\"quantity\":1},{" +
                "\"menuItemId\":\"" + second.getId() + "\",\"quantity\":2}]," +
                "\"deliveryAddress\":\"123 Main Street\"," +
                "\"deliveryLatitude\":10.7800000," +
                "\"deliveryLongitude\":106.7100000," +
                "\"customerNote\":\"no chili\"}";

        String idemKey = "checkout-key-001";

        String firstResponse = mockMvc.perform(post("/api/v1/customer/orders")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.paymentMethod").value("COD"))
                .andExpect(jsonPath("$.data.paymentStatus").value("UNPAID"))
                .andExpect(jsonPath("$.data.subtotalAmount").value(70000))
                .andExpect(jsonPath("$.data.deliveryFee").value(15000))
                .andExpect(jsonPath("$.data.totalAmount").value(85000))
                .andReturn().getResponse().getContentAsString();

        String secondResponse = mockMvc.perform(post("/api/v1/customer/orders")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String firstOrderId = extractField(firstResponse, "orderId");
        String secondOrderId = extractField(secondResponse, "orderId");

        org.junit.jupiter.api.Assertions.assertEquals(firstOrderId, secondOrderId);
        org.junit.jupiter.api.Assertions.assertEquals(1L, orderRepository.count());
    }

    private static String extractField(String response, String field) {
        String marker = "\"" + field + "\":\"";
        int start = response.indexOf(marker);
        int from = start + marker.length();
        int to = response.indexOf('"', from);
        return response.substring(from, to);
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
        return tokenService.issueAccessToken(saved, UUID.randomUUID().toString());
    }

    private Restaurant seedRestaurant(String name, BigDecimal lat, BigDecimal lng) {
        UserAccount owner = new UserAccount();
        owner.setUsername("owner-" + name.toLowerCase().replace(" ", "-"));
        owner.setEmail(name.toLowerCase().replace(" ", "") + "@merchant.test");
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
