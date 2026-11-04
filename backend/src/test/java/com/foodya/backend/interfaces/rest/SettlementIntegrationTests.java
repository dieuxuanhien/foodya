package com.foodya.backend.interfaces.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodya.backend.application.ports.out.GeoPort;
import com.foodya.backend.application.ports.out.TokenPort;
import com.foodya.backend.domain.entities.Order;
import com.foodya.backend.domain.entities.Restaurant;
import com.foodya.backend.domain.entities.UserAccount;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.value_objects.PaymentMethod;
import com.foodya.backend.domain.value_objects.PaymentStatus;
import com.foodya.backend.domain.value_objects.RestaurantStatus;
import com.foodya.backend.domain.value_objects.UserRole;
import com.foodya.backend.domain.value_objects.UserStatus;
import com.foodya.backend.infrastructure.mapper.OrderMapper;
import com.foodya.backend.infrastructure.mapper.RestaurantMapper;
import com.foodya.backend.infrastructure.mapper.UserAccountMapper;
import com.foodya.backend.infrastructure.persistence.models.UserAccountPersistenceModel;
import com.foodya.backend.infrastructure.repository.OrderRepository;
import com.foodya.backend.infrastructure.repository.RestaurantRepository;
import com.foodya.backend.infrastructure.repository.SettlementLineRepository;
import com.foodya.backend.infrastructure.repository.SettlementRepository;
import com.foodya.backend.infrastructure.repository.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Settlement flow end to end on the real queries: orders -> outstanding balance -> Done -> history.
 * Assertions look payees up by id, so rows left behind by other test classes cannot interfere.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SettlementIntegrationTests {

    private static final Random RANDOM = new Random();

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private OrderMapper orderMapper;
    @Autowired private RestaurantMapper restaurantMapper;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private SettlementRepository settlementRepository;
    @Autowired private SettlementLineRepository settlementLineRepository;
    @Autowired private TokenPort tokenService;
    @Autowired private GeoPort geoService;

    private final List<UUID> orderIds = new ArrayList<>();
    private final List<UUID> restaurantIds = new ArrayList<>();
    private final List<UUID> userIds = new ArrayList<>();

    @BeforeEach
    void clearLedger() {
        settlementLineRepository.deleteAll();
        settlementRepository.deleteAll();
    }

    @AfterEach
    void cleanUp() {
        settlementLineRepository.deleteAll();
        settlementRepository.deleteAll();
        orderRepository.deleteAllById(orderIds);
        restaurantRepository.deleteAllById(restaurantIds);
        userAccountRepository.deleteAllById(userIds);
        orderIds.clear();
        restaurantIds.clear();
        userIds.clear();
    }

    // ---------------------------------------------------------------- merchant

    @Test
    void merchantBalanceIsSubtotalMinusCommissionAndDoneClearsIt() throws Exception {
        UserAccount admin = seedUser("admin", UserRole.ADMIN);
        UserAccount merchant = seedUser("merchant", UserRole.MERCHANT);
        UserAccount customer = seedUser("customer", UserRole.CUSTOMER);
        UserAccount driver = seedUser("driver", UserRole.DELIVERY);
        Restaurant restaurant = seedRestaurant(merchant);
        seedOrder(customer, restaurant, driver, OrderStatus.SUCCESS, PaymentMethod.VNPAY, PaymentStatus.PAID, "100000", "15000", "15000");
        seedOrder(customer, restaurant, driver, OrderStatus.SUCCESS, PaymentMethod.COD, PaymentStatus.PAID, "100000", "15000", "15000");
        seedOrder(customer, restaurant, driver, OrderStatus.DELIVERING, PaymentMethod.COD, PaymentStatus.UNPAID, "100000", "15000", "15000");
        String adminToken = token(admin);

        JsonNode before = pendingEntry("MERCHANT", merchant.getId(), adminToken);
        assertThat(before).as("merchant with delivered orders is listed").isNotNull();
        assertThat(before.get("orderCount").asInt()).as("the undelivered order is excluded").isEqualTo(2);
        assertThat(before.get("grossAmount").decimalValue()).isEqualByComparingTo("200000");
        assertThat(before.get("platformFeeAmount").decimalValue()).isEqualByComparingTo("30000");
        assertThat(before.get("netAmount").decimalValue()).isEqualByComparingTo("170000");
        assertThat(before.get("direction").asText()).isEqualTo("PAYOUT");

        mockMvc.perform(post("/api/v1/admin/settlements/MERCHANT/{id}/settle", merchant.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedNetAmount\":170000,\"note\":\"week 45\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.settlement.direction").value("PAYOUT"))
                .andExpect(jsonPath("$.data.settlement.netAmount").value(170000))
                .andExpect(jsonPath("$.data.settlement.settledBy").value(admin.getId().toString()))
                .andExpect(jsonPath("$.data.lines.length()").value(2));

        assertThat(pendingEntry("MERCHANT", merchant.getId(), adminToken))
                .as("a settled merchant no longer has an outstanding balance").isNull();
        mockMvc.perform(get("/api/v1/admin/settlements/pending/MERCHANT/{id}", merchant.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderCount").value(0))
                .andExpect(jsonPath("$.data.netAmount").value(0));

        mockMvc.perform(get("/api/v1/admin/settlements")
                        .param("payeeType", "MERCHANT").param("payeeUserId", merchant.getId().toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].note").value("week 45"));

        mockMvc.perform(post("/api/v1/admin/settlements/MERCHANT/{id}/settle", merchant.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void refundedOrdersAreNotPaidToTheMerchantButTheDriverIsStillPaid() throws Exception {
        UserAccount admin = seedUser("admin", UserRole.ADMIN);
        UserAccount merchant = seedUser("merchant", UserRole.MERCHANT);
        UserAccount customer = seedUser("customer", UserRole.CUSTOMER);
        UserAccount driver = seedUser("driver", UserRole.DELIVERY);
        Restaurant restaurant = seedRestaurant(merchant);
        seedOrder(customer, restaurant, driver, OrderStatus.SUCCESS, PaymentMethod.VNPAY, PaymentStatus.REFUNDED, "100000", "15000", "15000");
        String adminToken = token(admin);

        assertThat(pendingEntry("MERCHANT", merchant.getId(), adminToken)).isNull();
        JsonNode driverEntry = pendingEntry("DRIVER", driver.getId(), adminToken);
        assertThat(driverEntry).isNotNull();
        assertThat(driverEntry.get("netAmount").decimalValue()).isEqualByComparingTo("15000");
    }

    @Test
    void settlingWithAStaleAmountIsRejectedAndNothingIsRecorded() throws Exception {
        UserAccount admin = seedUser("admin", UserRole.ADMIN);
        UserAccount merchant = seedUser("merchant", UserRole.MERCHANT);
        UserAccount customer = seedUser("customer", UserRole.CUSTOMER);
        Restaurant restaurant = seedRestaurant(merchant);
        seedOrder(customer, restaurant, null, OrderStatus.SUCCESS, PaymentMethod.COD, PaymentStatus.PAID, "100000", "15000", "15000");
        String adminToken = token(admin);

        mockMvc.perform(post("/api/v1/admin/settlements/MERCHANT/{id}/settle", merchant.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedNetAmount\":1}"))
                .andExpect(status().isConflict());

        assertThat(settlementRepository.count()).isZero();
        assertThat(pendingEntry("MERCHANT", merchant.getId(), adminToken)).isNotNull();
    }

    // ------------------------------------------------------------------ driver

    @Test
    void aDriverHoldingCodCashOwesThePlatformTheDifference() throws Exception {
        UserAccount admin = seedUser("admin", UserRole.ADMIN);
        UserAccount merchant = seedUser("merchant", UserRole.MERCHANT);
        UserAccount customer = seedUser("customer", UserRole.CUSTOMER);
        UserAccount driver = seedUser("driver", UserRole.DELIVERY);
        Restaurant restaurant = seedRestaurant(merchant);
        // total 115,000 collected in cash, of which the driver keeps their 15,000 delivery fee
        seedOrder(customer, restaurant, driver, OrderStatus.SUCCESS, PaymentMethod.COD, PaymentStatus.PAID, "100000", "15000", "15000");
        String adminToken = token(admin);

        JsonNode entry = pendingEntry("DRIVER", driver.getId(), adminToken);
        assertThat(entry.get("direction").asText()).isEqualTo("COLLECTION");
        assertThat(entry.get("cashCollectedAmount").decimalValue()).isEqualByComparingTo("115000");
        assertThat(entry.get("netAmount").decimalValue()).isEqualByComparingTo("100000");

        mockMvc.perform(post("/api/v1/admin/settlements/DRIVER/{id}/settle", driver.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedNetAmount\":100000,\"note\":\"cash handed in\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.settlement.direction").value("COLLECTION"));

        mockMvc.perform(get("/api/v1/delivery/settlements").header("Authorization", "Bearer " + token(driver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].direction").value("COLLECTION"));
    }

    @Test
    void driverBalanceNetsOnlineEarningsAgainstCodCash() throws Exception {
        UserAccount admin = seedUser("admin", UserRole.ADMIN);
        UserAccount merchant = seedUser("merchant", UserRole.MERCHANT);
        UserAccount customer = seedUser("customer", UserRole.CUSTOMER);
        UserAccount driver = seedUser("driver", UserRole.DELIVERY);
        Restaurant restaurant = seedRestaurant(merchant);
        seedOrder(customer, restaurant, driver, OrderStatus.SUCCESS, PaymentMethod.VNPAY, PaymentStatus.PAID, "100000", "15000", "15000");
        seedOrder(customer, restaurant, driver, OrderStatus.SUCCESS, PaymentMethod.VNPAY, PaymentStatus.PAID, "100000", "15000", "15000");
        seedOrder(customer, restaurant, driver, OrderStatus.SUCCESS, PaymentMethod.COD, PaymentStatus.PAID, "100000", "15000", "15000");

        // earned 3 x 15,000 = 45,000; holds 115,000 cash -> owes 70,000
        JsonNode entry = pendingEntry("DRIVER", driver.getId(), token(admin));
        assertThat(entry.get("orderCount").asInt()).isEqualTo(3);
        assertThat(entry.get("direction").asText()).isEqualTo("COLLECTION");
        assertThat(entry.get("netAmount").decimalValue()).isEqualByComparingTo("70000");
    }

    // ------------------------------------------------------------- access rules

    @Test
    void merchantsAndDriversSeeOnlyTheirOwnBalance() throws Exception {
        UserAccount merchant = seedUser("merchant", UserRole.MERCHANT);
        UserAccount otherMerchant = seedUser("merchant-other", UserRole.MERCHANT);
        UserAccount customer = seedUser("customer", UserRole.CUSTOMER);
        UserAccount driver = seedUser("driver", UserRole.DELIVERY);
        Restaurant restaurant = seedRestaurant(merchant);
        seedOrder(customer, restaurant, driver, OrderStatus.SUCCESS, PaymentMethod.VNPAY, PaymentStatus.PAID, "100000", "15000", "15000");

        mockMvc.perform(get("/api/v1/merchant/settlements/pending").header("Authorization", "Bearer " + token(merchant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.netAmount").value(85000))
                .andExpect(jsonPath("$.data.orderCount").value(1));
        mockMvc.perform(get("/api/v1/merchant/settlements/pending").header("Authorization", "Bearer " + token(otherMerchant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderCount").value(0));
        mockMvc.perform(get("/api/v1/delivery/settlements/pending").header("Authorization", "Bearer " + token(driver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.netAmount").value(15000))
                .andExpect(jsonPath("$.data.direction").value("PAYOUT"));
    }

    @Test
    void onlyAdminsCanViewOrSettleBalances() throws Exception {
        UserAccount merchant = seedUser("merchant", UserRole.MERCHANT);
        UserAccount customer = seedUser("customer", UserRole.CUSTOMER);

        mockMvc.perform(post("/api/v1/admin/settlements/MERCHANT/{id}/settle", merchant.getId())
                        .header("Authorization", "Bearer " + token(merchant)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/settlements/pending").param("payeeType", "MERCHANT")
                        .header("Authorization", "Bearer " + token(customer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void anUnknownPayeeTypeIsAValidationErrorNotAServerError() throws Exception {
        UserAccount admin = seedUser("admin", UserRole.ADMIN);

        mockMvc.perform(get("/api/v1/admin/settlements/pending").param("payeeType", "CUSTOMER")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isUnprocessableEntity());
    }

    // ----------------------------------------------------------------- helpers

    private JsonNode pendingEntry(String payeeType, UUID payeeId, String token) throws Exception {
        String body = mockMvc.perform(get("/api/v1/admin/settlements/pending").param("payeeType", payeeType)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode node : objectMapper.readTree(body).get("data")) {
            if (payeeId.toString().equals(node.get("payeeUserId").asText())) {
                return node;
            }
        }
        return null;
    }

    private String token(UserAccount user) {
        return tokenService.issueAccessToken(user, UUID.randomUUID().toString());
    }

    private UserAccount seedUser(String stem, UserRole role) {
        String unique = stem + "-" + UUID.randomUUID().toString().substring(0, 8);
        UserAccountPersistenceModel user = new UserAccountPersistenceModel();
        user.setUsername(unique);
        user.setEmail(unique + "@test.local");
        user.setPhoneNumber("+8490" + (1000000 + RANDOM.nextInt(9000000)));
        user.setFullName(stem + " person");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash("$2a$10$abcdefghijklmnopqrstuv");
        UserAccount saved = new UserAccountMapper().toDomain(userAccountRepository.save(user));
        userIds.add(saved.getId());
        return saved;
    }

    private Restaurant seedRestaurant(UserAccount owner) {
        Restaurant restaurant = new Restaurant();
        restaurant.setOwnerUserId(owner.getId());
        restaurant.setName("Settle Store " + UUID.randomUUID().toString().substring(0, 6));
        restaurant.setCuisineType("Vietnamese");
        restaurant.setDescription("settlement test");
        restaurant.setAddressLine("123 Test Street");
        restaurant.setLatitude(new BigDecimal("10.7700000"));
        restaurant.setLongitude(new BigDecimal("106.7000000"));
        restaurant.setH3IndexRes9(geoService.h3Res9(10.77, 106.7));
        restaurant.setStatus(RestaurantStatus.ACTIVE);
        restaurant.setOpen(true);
        restaurant.setMaxDeliveryKm(new BigDecimal("8.0"));
        Restaurant saved = restaurantMapper.toDomain(restaurantRepository.save(restaurantMapper.toPersistence(restaurant)));
        restaurantIds.add(saved.getId());
        return saved;
    }

    private Order seedOrder(UserAccount customer, Restaurant restaurant, UserAccount driver, OrderStatus status,
                            PaymentMethod method, PaymentStatus paymentStatus,
                            String subtotal, String deliveryFee, String commission) {
        BigDecimal sub = new BigDecimal(subtotal);
        BigDecimal fee = new BigDecimal(deliveryFee);
        Order order = new Order();
        order.setOrderCode("ST-" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase());
        order.setCustomerUserId(customer.getId());
        order.setIdempotencyKey(UUID.randomUUID().toString());
        order.setRestaurantId(restaurant.getId());
        order.setDriverUserId(driver == null ? null : driver.getId());
        order.setStatus(status);
        order.setDeliveryAddress("10 Main St");
        order.setDeliveryLatitude(new BigDecimal("10.7800000"));
        order.setDeliveryLongitude(new BigDecimal("106.7100000"));
        order.setCustomerNote("none");
        order.setSubtotalAmount(sub);
        order.setDeliveryFee(fee);
        order.setTotalAmount(sub.add(fee));
        order.setPaymentMethod(method);
        order.setPaymentStatus(paymentStatus);
        order.setCommissionAmount(new BigDecimal(commission));
        order.setShippingFeeMarginAmount(BigDecimal.ZERO);
        order.setPlatformProfitAmount(new BigDecimal(commission));
        if (status == OrderStatus.SUCCESS) {
            order.setCompletedAt(OffsetDateTime.now());
        }
        Order saved = orderMapper.toDomain(orderRepository.save(orderMapper.toPersistence(order)));
        orderIds.add(saved.getId());
        return saved;
    }
}