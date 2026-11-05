package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.ports.out.TokenPort;
import com.foodya.backend.domain.entities.UserAccount;
import com.foodya.backend.domain.value_objects.NotificationReceiverType;
import com.foodya.backend.domain.value_objects.NotificationStatus;
import com.foodya.backend.domain.value_objects.UserRole;
import com.foodya.backend.domain.value_objects.UserStatus;
import com.foodya.backend.infrastructure.mapper.AuthPersistenceMapper;
import com.foodya.backend.infrastructure.persistence.models.NotificationLogPersistenceModel;
import com.foodya.backend.infrastructure.repository.CartItemRepository;
import com.foodya.backend.infrastructure.repository.CartRepository;
import com.foodya.backend.infrastructure.repository.DeviceTokenRepository;
import com.foodya.backend.infrastructure.repository.MenuCategoryRepository;
import com.foodya.backend.infrastructure.repository.MenuItemRepository;
import com.foodya.backend.infrastructure.repository.NotificationLogRepository;
import com.foodya.backend.infrastructure.repository.OrderItemRepository;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerNotificationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenPort tokenService;

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private MenuCategoryRepository menuCategoryRepository;

    @BeforeEach
    void setUp() {
        deviceTokenRepository.deleteAll();
        notificationLogRepository.deleteAll();
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
    void customerCanListOwnNotificationsAndMarkAsRead() throws Exception {
        UserAccount customer = seedUser("customer-noti-a", UserRole.CUSTOMER);
        UserAccount otherCustomer = seedUser("customer-noti-b", UserRole.CUSTOMER);

        NotificationLogPersistenceModel target = seedNotification(customer.getId(), "ORDER_ACCEPTED", "Order accepted");
        seedNotification(customer.getId(), "ORDER_PREPARING", "Kitchen is preparing your order");
        seedNotification(otherCustomer.getId(), "ORDER_CANCELLED", "Order cancelled");

        String customerToken = tokenService.issueAccessToken(
                customer,
                UUID.randomUUID().toString()
        );

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.totalElements").value(2))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].receiverUserId").value(customer.getId().toString()));

        mockMvc.perform(patch("/api/v1/notifications/{id}/read", target.getId())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(target.getId().toString()))
                .andExpect(jsonPath("$.data.readAt").isNotEmpty());
    }

    @Test
    void customerCannotMarkAnotherUsersNotificationAsRead() throws Exception {
        UserAccount customer = seedUser("customer-noti-owner", UserRole.CUSTOMER);
        UserAccount otherCustomer = seedUser("customer-noti-other", UserRole.CUSTOMER);

        NotificationLogPersistenceModel target = seedNotification(customer.getId(), "ORDER_READY", "Order is ready for pickup");

        String otherCustomerToken = tokenService.issueAccessToken(
                otherCustomer,
                UUID.randomUUID().toString()
        );

        mockMvc.perform(patch("/api/v1/notifications/{id}/read", target.getId())
                        .header("Authorization", "Bearer " + otherCustomerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("notification not found"));
    }

    @Test
    void authenticatedUserCanRegisterAndUnregisterDeviceToken() throws Exception {
        UserAccount merchant = seedUser("merchant-device-token", UserRole.MERCHANT);
        String merchantToken = tokenService.issueAccessToken(
                merchant,
                UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/api/v1/notifications/devices")
                        .header("Authorization", "Bearer " + merchantToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "token": "fcm-token-merchant-1",
                                  "platform": "ANDROID",
                                  "deviceId": "emulator-5554",
                                  "appVersion": "1.0.0"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(merchant.getId().toString()))
                .andExpect(jsonPath("$.data.platform").value("ANDROID"))
                .andExpect(jsonPath("$.data.enabled").value(true));

        mockMvc.perform(delete("/api/v1/notifications/devices")
                        .header("Authorization", "Bearer " + merchantToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "token": "fcm-token-merchant-1"
                                }
                                """))
                .andExpect(status().isOk());
    }

    private UserAccount seedUser(String stem, UserRole role) {
        com.foodya.backend.infrastructure.persistence.models.UserAccountPersistenceModel user = new com.foodya.backend.infrastructure.persistence.models.UserAccountPersistenceModel();
        user.setUsername(stem);
        user.setEmail(stem + "@test.local");
        user.setPhoneNumber("+8490" + Math.abs(stem.hashCode() % 10000000));
        user.setFullName(stem);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash("$2a$10$abcdefghijklmnopqrstuv");
        return new com.foodya.backend.infrastructure.mapper.UserAccountMapper().toDomain(userAccountRepository.save(user));
    }

    private NotificationLogPersistenceModel seedNotification(UUID receiverUserId, String eventType, String title) {
        NotificationLogPersistenceModel log = new NotificationLogPersistenceModel();
        log.setReceiverUserId(receiverUserId);
        log.setReceiverType(NotificationReceiverType.CUSTOMER);
        log.setEventType(eventType);
        log.setTitle(title);
        log.setMessage(title + " message");
        log.setStatus(NotificationStatus.SENT);
        return notificationLogRepository.save(log);
    }
}
