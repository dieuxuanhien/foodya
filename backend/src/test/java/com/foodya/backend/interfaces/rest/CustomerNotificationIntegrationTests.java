package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.ports.out.TokenPort;
import com.foodya.backend.domain.entities.NotificationLog;
import com.foodya.backend.domain.entities.UserAccount;
import com.foodya.backend.domain.value_objects.NotificationReceiverType;
import com.foodya.backend.domain.value_objects.NotificationStatus;
import com.foodya.backend.domain.value_objects.UserRole;
import com.foodya.backend.domain.value_objects.UserStatus;
import com.foodya.backend.infrastructure.adapter.mapper.AuthPersistenceMapper;
import com.foodya.backend.infrastructure.repository.CartItemRepository;
import com.foodya.backend.infrastructure.repository.CartRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

        NotificationLog target = seedNotification(customer.getId(), "ORDER_ACCEPTED", "Order accepted");
        seedNotification(customer.getId(), "ORDER_PREPARING", "Kitchen is preparing your order");
        seedNotification(otherCustomer.getId(), "ORDER_CANCELLED", "Order cancelled");

        String customerToken = tokenService.issueAccessToken(
                AuthPersistenceMapper.toModel(customer),
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

        NotificationLog target = seedNotification(customer.getId(), "ORDER_READY", "Order is ready for pickup");

        String otherCustomerToken = tokenService.issueAccessToken(
                AuthPersistenceMapper.toModel(otherCustomer),
                UUID.randomUUID().toString()
        );

        mockMvc.perform(patch("/api/v1/notifications/{id}/read", target.getId())
                        .header("Authorization", "Bearer " + otherCustomerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("notification not found"));
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

    private NotificationLog seedNotification(UUID receiverUserId, String eventType, String title) {
        NotificationLog log = new NotificationLog();
        log.setReceiverUserId(receiverUserId);
        log.setReceiverType(NotificationReceiverType.CUSTOMER);
        log.setEventType(eventType);
        log.setTitle(title);
        log.setMessage(title + " message");
        log.setStatus(NotificationStatus.SENT);
        return notificationLogRepository.save(log);
    }
}
