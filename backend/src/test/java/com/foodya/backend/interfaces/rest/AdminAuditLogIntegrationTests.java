package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.ports.out.TokenPort;
import com.foodya.backend.domain.entities.UserAccount;
import com.foodya.backend.domain.value_objects.UserRole;
import com.foodya.backend.domain.value_objects.UserStatus;
import com.foodya.backend.infrastructure.mapper.AuthPersistenceMapper;
import com.foodya.backend.infrastructure.mapper.UserAccountMapper;
import com.foodya.backend.infrastructure.persistence.models.AuditLogPersistenceModel;
import com.foodya.backend.infrastructure.persistence.models.UserAccountPersistenceModel;
import com.foodya.backend.infrastructure.repository.AuditLogRepository;
import com.foodya.backend.infrastructure.repository.CartItemRepository;
import com.foodya.backend.infrastructure.repository.CartRepository;
import com.foodya.backend.infrastructure.repository.MenuCategoryRepository;
import com.foodya.backend.infrastructure.repository.MenuItemRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAuditLogIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenPort tokenPort;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private MenuCategoryRepository menuCategoryRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @BeforeEach
    void cleanData() {
        auditLogRepository.deleteAll();
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
    void adminCanListAuditLogsWithFilters() throws Exception {
        UserAccount admin = seedUser("admin-audit", UserRole.ADMIN);
        String adminToken = tokenPort.issueAccessToken(AuthPersistenceMapper.toData(admin), UUID.randomUUID().toString());

        seedAuditLog("ORDER_STATUS_UPDATED", "ORDER", "order-1001", "PENDING", "ACCEPTED", "actor-a");
        seedAuditLog("ORDER_CANCELLED", "ORDER", "order-1002", "ASSIGNED", "CANCELLED", "actor-b");

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("eventType", "ORDER_STATUS_UPDATED")
                        .param("entityType", "ORDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.totalElements").value(1))
                .andExpect(jsonPath("$.data[0].eventType").value("ORDER_STATUS_UPDATED"))
                .andExpect(jsonPath("$.data[0].entityType").value("ORDER"))
                .andExpect(jsonPath("$.data[0].entityId").value("order-1001"));
    }

    private UserAccount seedUser(String stem, UserRole role) {
        UserAccountPersistenceModel user = new UserAccountPersistenceModel();
        user.setUsername(stem);
        user.setEmail(stem + "@test.local");
        user.setPhoneNumber("+8490" + Math.abs(stem.hashCode() % 10000000));
        user.setFullName(stem);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash("$2a$10$abcdefghijklmnopqrstuv");
        return new UserAccountMapper().toDomain(userAccountRepository.save(user));
    }

    private void seedAuditLog(String action,
                              String targetType,
                              String targetId,
                              String oldValue,
                              String newValue,
                              String actorUserId) {
        AuditLogPersistenceModel model = new AuditLogPersistenceModel();
        model.setAction(action);
        model.setTargetType(targetType);
        model.setTargetId(targetId);
        model.setOldValue(oldValue);
        model.setNewValue(newValue);
        model.setActorUserId(actorUserId);
        auditLogRepository.save(model);
    }
}
