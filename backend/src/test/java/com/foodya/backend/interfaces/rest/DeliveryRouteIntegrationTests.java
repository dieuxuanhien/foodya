package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.ports.out.DeliveryRoutePort;
import com.foodya.backend.application.ports.out.TokenPort;
import com.foodya.backend.domain.entities.UserAccount;
import com.foodya.backend.domain.value_objects.*;
import com.foodya.backend.infrastructure.mapper.UserAccountMapper;
import com.foodya.backend.infrastructure.persistence.models.*;
import com.foodya.backend.infrastructure.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the Phase 6 route endpoints
 * (GET /customer/orders/{id}/route, GET /delivery/orders/{id}/route).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeliveryRouteIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private TokenPort tokenService;
    @Autowired private UserAccountMapper userAccountMapper;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private OrderManagementRepository orderManagementRepository;
    @Autowired private DeliveryRoutePort deliveryRoutePort;

    private UserAccount customer;
    private UserAccount driver;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        customer = seedUser("route-cust", UserRole.CUSTOMER);
        driver = seedUser("route-drv", UserRole.DELIVERY);

        RestaurantPersistenceModel restaurant = new RestaurantPersistenceModel();
        restaurant.setId(UUID.randomUUID());
        restaurant.setOwnerUserId(driver.getId());
        restaurant.setName("Route Restaurant");
        restaurant.setCuisineType("vietnamese");
        restaurant.setAddressLine("123 Test St");
        restaurant.setLatitude(new BigDecimal("10.762622"));
        restaurant.setLongitude(new BigDecimal("106.660172"));
        restaurant.setH3IndexRes9("8929a4825ffffff");
        restaurant.setMaxDeliveryKm(new BigDecimal("5.0"));
        restaurant.setStatus(RestaurantStatus.APPROVED);
        restaurant.setCreatedAt(OffsetDateTime.now());
        restaurant = restaurantRepository.save(restaurant);

        OrderPersistenceModel order = new OrderPersistenceModel();
        order.setId(UUID.randomUUID());
        order.setOrderCode("ROUTE-" + System.nanoTime());
        order.setIdempotencyKey(UUID.randomUUID().toString());
        order.setCustomerUserId(customer.getId());
        order.setRestaurantId(restaurant.getId());
        order.setDriverUserId(driver.getId());
        order.setStatus(OrderStatus.ASSIGNED);
        order.setDeliveryAddress("456 Customer St");
        order.setDeliveryLatitude(new BigDecimal("10.772622"));
        order.setDeliveryLongitude(new BigDecimal("106.670172"));
        order.setSubtotalAmount(new BigDecimal("50000"));
        order.setDeliveryFee(new BigDecimal("15000"));
        order.setTotalAmount(new BigDecimal("65000"));
        order.setCommissionAmount(BigDecimal.ZERO);
        order.setShippingFeeMarginAmount(BigDecimal.ZERO);
        order.setPlatformProfitAmount(BigDecimal.ZERO);
        order.setPaymentMethod(PaymentMethod.COD);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setPlacedAt(OffsetDateTime.now());
        order = orderManagementRepository.save(order);
        orderId = order.getId();
    }

    @Test
    void customerGetsForbiddenBeforeAnyRouteIsPlanned() throws Exception {
        String token = tokenService.issueAccessToken(customer, UUID.randomUUID().toString());
        mockMvc.perform(get("/api/v1/customer/orders/" + orderId + "/route")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void routeIsReturnedForOwnerWithWaypoints() throws Exception {
        // seed an active route directly (route planning itself is exercised by DeliveryRouteService's callers)
        deliveryRoutePort.save(com.foodya.backend.domain.entities.DeliveryRoute.create(
                orderId, null, "_p~iF~ps|U_ulLnnqC_mqNvxq`@", 1200, 300,
                java.util.List.of(
                        new RouteWaypoint(10.762622, 106.660172, RouteWaypoint.PICKUP, orderId),
                        new RouteWaypoint(10.772622, 106.670172, RouteWaypoint.DROPOFF, orderId))));

        String token = tokenService.issueAccessToken(customer, UUID.randomUUID().toString());
        mockMvc.perform(get("/api/v1/customer/orders/" + orderId + "/route")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.encodedPolyline").value("_p~iF~ps|U_ulLnnqC_mqNvxq`@"))
                .andExpect(jsonPath("$.data.distanceMeters").value(1200))
                .andExpect(jsonPath("$.data.durationSeconds").value(300))
                .andExpect(jsonPath("$.data.waypoints.length()").value(2))
                .andExpect(jsonPath("$.data.waypoints[0].type").value("PICKUP"))
                .andExpect(jsonPath("$.data.waypoints[1].type").value("DROPOFF"));
    }

    @Test
    void otherCustomerCannotReadRoute() throws Exception {
        deliveryRoutePort.save(com.foodya.backend.domain.entities.DeliveryRoute.create(
                orderId, null, "_p", 10, 10, java.util.List.of()));
        UserAccount other = seedUser("route-other-cust", UserRole.CUSTOMER);
        String token = tokenService.issueAccessToken(other, UUID.randomUUID().toString());
        mockMvc.perform(get("/api/v1/customer/orders/" + orderId + "/route")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignedDriverCanReadRoute() throws Exception {
        deliveryRoutePort.save(com.foodya.backend.domain.entities.DeliveryRoute.create(
                orderId, null, "_p", 10, 10, java.util.List.of()));
        String token = tokenService.issueAccessToken(driver, UUID.randomUUID().toString());
        mockMvc.perform(get("/api/v1/delivery/orders/" + orderId + "/route")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.distanceMeters", greaterThan(0)));
    }

    @Test
    void unassignedDriverCannotReadRoute() throws Exception {
        deliveryRoutePort.save(com.foodya.backend.domain.entities.DeliveryRoute.create(
                orderId, null, "_p", 10, 10, java.util.List.of()));
        UserAccount otherDriver = seedUser("route-other-drv", UserRole.DELIVERY);
        String token = tokenService.issueAccessToken(otherDriver, UUID.randomUUID().toString());
        mockMvc.perform(get("/api/v1/delivery/orders/" + orderId + "/route")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private UserAccount seedUser(String stem, UserRole role) {
        UserAccountPersistenceModel model = new UserAccountPersistenceModel();
        model.setUsername(stem + "-" + System.nanoTime());
        model.setEmail(stem + "-" + System.nanoTime() + "@test.local");
        model.setPhoneNumber("+8490" + String.format("%07d", Math.abs(System.nanoTime() % 10000000)));
        model.setFullName(stem);
        model.setRole(role);
        model.setStatus(UserStatus.ACTIVE);
        model.setPasswordHash("$2a$10$abcdefghijklmnopqrstuv");
        return userAccountMapper.toDomain(userAccountRepository.save(model));
    }
}