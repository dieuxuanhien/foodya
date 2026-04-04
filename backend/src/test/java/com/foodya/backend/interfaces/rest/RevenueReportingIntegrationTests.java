package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.ports.out.TokenPort;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.infrastructure.mapper.AuthPersistenceMapper;
import com.foodya.backend.domain.value_objects.PaymentMethod;
import com.foodya.backend.domain.value_objects.PaymentStatus;
import com.foodya.backend.domain.value_objects.RestaurantStatus;
import com.foodya.backend.domain.value_objects.UserRole;
import com.foodya.backend.domain.value_objects.UserStatus;
import com.foodya.backend.domain.entities.Order;
import com.foodya.backend.domain.entities.OrderItem;
import com.foodya.backend.domain.entities.MenuCategory;
import com.foodya.backend.domain.entities.MenuItem;
import com.foodya.backend.domain.entities.Restaurant;
import com.foodya.backend.domain.entities.UserAccount;
import com.foodya.backend.infrastructure.repository.MenuCategoryRepository;
import com.foodya.backend.infrastructure.repository.MenuItemRepository;
import com.foodya.backend.infrastructure.repository.CartItemRepository;
import com.foodya.backend.infrastructure.repository.CartRepository;
import com.foodya.backend.infrastructure.repository.OrderItemRepository;
import com.foodya.backend.infrastructure.repository.OrderRepository;
import com.foodya.backend.infrastructure.repository.RestaurantRepository;
import com.foodya.backend.infrastructure.repository.UserAccountRepository;
import com.foodya.backend.infrastructure.mapper.MenuCategoryMapper;
import com.foodya.backend.infrastructure.mapper.MenuItemMapper;
import com.foodya.backend.infrastructure.mapper.OrderItemMapper;
import com.foodya.backend.infrastructure.mapper.OrderMapper;
import com.foodya.backend.infrastructure.mapper.RestaurantMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RevenueReportingIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private MenuCategoryRepository menuCategoryRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private TokenPort tokenService;

    @Autowired
    private RestaurantMapper restaurantMapper;

    @Autowired
    private MenuCategoryMapper menuCategoryMapper;

    @Autowired
    private MenuItemMapper menuItemMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        orderItemRepository.deleteAll();
        menuItemRepository.deleteAll();
        menuCategoryRepository.deleteAll();
        orderRepository.deleteAll();
        restaurantRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void adminAndMerchantRevenueReportsReturnExpectedData() throws Exception {
        UserAccount admin = saveUser("admin_report_01", "admin_report_01@foodya.test", "+84991110001", UserRole.ADMIN);
        UserAccount merchantOne = saveUser("merchant_report_01", "merchant_report_01@foodya.test", "+84991110002", UserRole.MERCHANT);
        UserAccount merchantTwo = saveUser("merchant_report_02", "merchant_report_02@foodya.test", "+84991110003", UserRole.MERCHANT);
        UserAccount customer = saveUser("customer_report_01", "customer_report_01@foodya.test", "+84991110004", UserRole.CUSTOMER);

        Restaurant firstRestaurant = saveRestaurant(merchantOne, "Merchant One Store");
        Restaurant secondRestaurant = saveRestaurant(merchantTwo, "Merchant Two Store");
        MenuCategory firstCategory = saveCategory(firstRestaurant, "Main");
        MenuCategory secondCategory = saveCategory(secondRestaurant, "Main");
        MenuItem phoItem = saveMenuItem(firstRestaurant, firstCategory, "Pho", new BigDecimal("50000.00"));
        MenuItem teaItem = saveMenuItem(firstRestaurant, firstCategory, "Tea", new BigDecimal("15000.00"));
        MenuItem burgerItem = saveMenuItem(secondRestaurant, secondCategory, "Burger", new BigDecimal("90000.00"));

        Order successOne = saveOrder(customer, firstRestaurant, OrderStatus.SUCCESS, "ODR-REPORT-0001", new BigDecimal("115000.00"), new BigDecimal("12000.00"));
        saveOrderItem(successOne, phoItem, 2, new BigDecimal("100000.00"));

        Order successTwo = saveOrder(customer, firstRestaurant, OrderStatus.SUCCESS, "ODR-REPORT-0002", new BigDecimal("60000.00"), new BigDecimal("7000.00"));
        saveOrderItem(successTwo, teaItem, 3, new BigDecimal("45000.00"));

        Order otherMerchantSuccess = saveOrder(customer, secondRestaurant, OrderStatus.SUCCESS, "ODR-REPORT-0003", new BigDecimal("90000.00"), new BigDecimal("9000.00"));
        saveOrderItem(otherMerchantSuccess, burgerItem, 1, new BigDecimal("90000.00"));
        saveOrder(customer, firstRestaurant, OrderStatus.PENDING, "ODR-REPORT-0004", new BigDecimal("50000.00"), new BigDecimal("5000.00"));

        String adminToken = tokenService.issueAccessToken(AuthPersistenceMapper.toModel(admin), UUID.randomUUID().toString());
        String merchantOneToken = tokenService.issueAccessToken(AuthPersistenceMapper.toModel(merchantOne), UUID.randomUUID().toString());

        mockMvc.perform(get("/api/v1/admin/reports/revenue")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderCount").value(3))
                .andExpect(jsonPath("$.data.revenue").value(265000))
                .andExpect(jsonPath("$.data.platformProfit").value(28000));

        mockMvc.perform(get("/api/v1/merchant/reports/revenue")
                        .header("Authorization", "Bearer " + merchantOneToken)
                        .param("topItems", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderCount").value(2))
                .andExpect(jsonPath("$.data.revenue").value(175000))
                .andExpect(jsonPath("$.data.topSellingItems[0].itemName").value("Tea"))
                .andExpect(jsonPath("$.data.topSellingItems[0].quantitySold").value(3));
    }

    @Test
    void merchantRevenueReportRejectsInvalidTopItems() throws Exception {
        UserAccount merchant = saveUser("merchant_report_03", "merchant_report_03@foodya.test", "+84991110005", UserRole.MERCHANT);
        String token = tokenService.issueAccessToken(AuthPersistenceMapper.toModel(merchant), UUID.randomUUID().toString());

        mockMvc.perform(get("/api/v1/merchant/reports/revenue")
                        .header("Authorization", "Bearer " + token)
                        .param("topItems", "0"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private UserAccount saveUser(String username, String email, String phone, UserRole role) {
        com.foodya.backend.infrastructure.persistence.models.UserAccountPersistenceModel user = new com.foodya.backend.infrastructure.persistence.models.UserAccountPersistenceModel();
        user.setUsername(username);
        user.setEmail(email);
        user.setPhoneNumber(phone);
        user.setFullName(username);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash("$2a$10$abcdefghijklmnopqrstuv");
        return new com.foodya.backend.infrastructure.mapper.UserAccountMapper().toDomain(userAccountRepository.save(user));
    }

    private Restaurant saveRestaurant(UserAccount owner, String name) {
        Restaurant restaurant = new Restaurant();
        restaurant.setOwnerUserId(owner.getId());
        restaurant.setName(name);
        restaurant.setCuisineType("Vietnamese");
        restaurant.setDescription("test");
        restaurant.setAddressLine("123 Test Street");
        restaurant.setLatitude(new BigDecimal("10.7750000"));
        restaurant.setLongitude(new BigDecimal("106.7000000"));
        restaurant.setH3IndexRes9("8928308280fffff");
        restaurant.setStatus(RestaurantStatus.ACTIVE);
        restaurant.setOpen(true);
        restaurant.setMaxDeliveryKm(new BigDecimal("10.000"));
        var persistenceModel = restaurantMapper.toPersistence(restaurant);
        @SuppressWarnings("null")
        var saved = restaurantRepository.save(persistenceModel);
        return restaurantMapper.toDomain(saved);
    }

    private Order saveOrder(UserAccount customer,
                            Restaurant restaurant,
                            OrderStatus status,
                            String orderCode,
                            BigDecimal totalAmount,
                            BigDecimal platformProfit) {
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
        order.setSubtotalAmount(totalAmount.subtract(new BigDecimal("15000.00")));
        order.setDeliveryFee(new BigDecimal("15000.00"));
        order.setTotalAmount(totalAmount);
        order.setPaymentMethod(PaymentMethod.COD);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setCommissionAmount(platformProfit);
        order.setShippingFeeMarginAmount(BigDecimal.ZERO);
        order.setPlatformProfitAmount(platformProfit);
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

    private MenuItem saveMenuItem(Restaurant restaurant, MenuCategory category, String name, BigDecimal price) {
        MenuItem item = new MenuItem();
        item.setRestaurantId(restaurant.getId());
        item.setCategoryId(category.getId());
        item.setName(name);
        item.setDescription(name + " item");
        item.setPrice(price);
        item.setActive(true);
        item.setAvailable(true);
        var persistenceModel = menuItemMapper.toPersistence(item);
        @SuppressWarnings("null")
        var saved = menuItemRepository.save(persistenceModel);
        return menuItemMapper.toDomain(saved);
    }

    private void saveOrderItem(Order order, MenuItem menuItem, int quantity, BigDecimal lineTotal) {
        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setMenuItemId(menuItem.getId());
        item.setMenuItemNameSnapshot(menuItem.getName());
        item.setUnitPriceSnapshot(lineTotal.divide(BigDecimal.valueOf(quantity), 2, java.math.RoundingMode.HALF_UP));
        item.setQuantity(quantity);
        item.setLineTotal(lineTotal);
        orderItemRepository.save(Objects.requireNonNull(orderItemMapper.toPersistence(item)));
    }
}
