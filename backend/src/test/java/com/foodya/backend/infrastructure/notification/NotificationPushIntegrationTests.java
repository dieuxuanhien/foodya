package com.foodya.backend.infrastructure.notification;

import com.foodya.backend.domain.value_objects.NotificationStatus;
import com.foodya.backend.domain.value_objects.UserRole;
import com.foodya.backend.domain.value_objects.UserStatus;
import com.foodya.backend.infrastructure.mapper.UserAccountMapper;
import com.foodya.backend.infrastructure.persistence.models.UserAccountPersistenceModel;
import com.foodya.backend.infrastructure.repository.NotificationLogRepository;
import com.foodya.backend.infrastructure.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;
import com.foodya.backend.application.usecases.NotificationService;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class NotificationPushIntegrationTests {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @BeforeEach
    void setUp() {
        notificationLogRepository.deleteAll();
    }

    @Test
    void notifyUser_persistsSkippedStatusWhenDeviceTokenIsNotConfigured() {
        UUID receiverId = seedUser("push-it-customer").getId();

        var result = notificationService.notifyUser(
                receiverId,
                UserRole.CUSTOMER,
                "ORDER_CREATED",
                "Order received",
                "Your order has been created",
            null
        );

        assertEquals(NotificationStatus.SKIPPED, result.status());
        assertEquals(1L, notificationLogRepository.count());

        var saved = notificationLogRepository.findAll().getFirst();
        assertEquals(receiverId, saved.getReceiverUserId());
        assertEquals(NotificationStatus.SKIPPED, saved.getStatus());
        assertEquals("device-token-not-configured", saved.getProviderResponse());
    }

    private com.foodya.backend.domain.entities.UserAccount seedUser(String stem) {
        String uniqueStem = stem + "-" + UUID.randomUUID().toString().substring(0, 8);
        UserAccountPersistenceModel user = new UserAccountPersistenceModel();
        user.setUsername(uniqueStem);
        user.setEmail(uniqueStem + "@test.local");
        user.setPhoneNumber("+8490" + Math.abs(uniqueStem.hashCode() % 10000000));
        user.setFullName(uniqueStem);
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash("$2a$10$abcdefghijklmnopqrstuv");
        return new UserAccountMapper().toDomain(userAccountRepository.save(user));
    }
}
