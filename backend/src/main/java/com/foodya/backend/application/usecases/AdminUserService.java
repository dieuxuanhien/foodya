package com.foodya.backend.application.usecases;

import com.foodya.backend.application.dto.AdminUserSummaryView;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.exception.ConflictException;
import com.foodya.backend.application.exception.NotFoundException;
import com.foodya.backend.application.ports.in.AdminUserUseCase;
import com.foodya.backend.application.ports.out.AdminUserPort;
import com.foodya.backend.application.support.PaginationPolicy;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.value_objects.UserStatus;
import com.foodya.backend.domain.entities.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AdminUserService implements AdminUserUseCase {

    private static final List<OrderStatus> BLOCKING_DELETE_STATUSES = List.of(
            OrderStatus.PENDING,
            OrderStatus.ACCEPTED,
            OrderStatus.ASSIGNED,
            OrderStatus.PREPARING,
            OrderStatus.DELIVERING
    );

    private final AdminUserPort adminUserPort;
    private final PaginationPolicy paginationPolicy;
    private final AuditLogService auditLogService;

    public AdminUserService(AdminUserPort adminUserPort,
                            PaginationPolicy paginationPolicy,
                            AuditLogService auditLogService) {
        this.adminUserPort = adminUserPort;
        this.paginationPolicy = paginationPolicy;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public PaginatedResult<AdminUserSummaryView> list(String keyword, Integer page, Integer size) {
        PaginationPolicy.PaginationSpec spec = paginationPolicy.page(page, size);
        PaginatedResult<UserAccount> users = adminUserPort.search(keyword, spec.page(), spec.size());

        return new PaginatedResult<>(
                users.items().stream().map(this::toView).toList(),
                users.page(),
                users.size(),
                users.totalElements(),
                users.totalPages()
        );
    }

    @Transactional
    public AdminUserSummaryView lock(UUID userId, UUID actorId) {
        UserAccount user = requireUser(userId);
        UserStatus oldStatus = user.getStatus();

        user.setStatus(UserStatus.LOCKED);
        UserAccount updated = adminUserPort.save(user);

        auditLogService.securityEvent(
                actorId.toString(),
                "ADMIN_USER_LOCK",
                "USER",
                userId.toString(),
                oldStatus.name(),
                updated.getStatus().name()
        );

        return toView(updated);
    }

    @Transactional
    public AdminUserSummaryView unlock(UUID userId, UUID actorId) {
        UserAccount user = requireUser(userId);
        UserStatus oldStatus = user.getStatus();

        user.setStatus(UserStatus.ACTIVE);
        UserAccount updated = adminUserPort.save(user);

        auditLogService.securityEvent(
                actorId.toString(),
                "ADMIN_USER_UNLOCK",
                "USER",
                userId.toString(),
                oldStatus.name(),
                updated.getStatus().name()
        );

        return toView(updated);
    }

    @Transactional
    public void delete(UUID userId, UUID actorId) {
        UserAccount user = requireUser(userId);

        boolean hasCustomerOrders = adminUserPort.hasCustomerOrdersInStatuses(userId, BLOCKING_DELETE_STATUSES);
        boolean hasMerchantOrders = adminUserPort.hasMerchantOrdersInStatuses(userId, BLOCKING_DELETE_STATUSES);
        if (hasCustomerOrders || hasMerchantOrders) {
            throw new ConflictException("hard delete blocked by linked active orders");
        }

        adminUserPort.delete(user);
        auditLogService.securityEvent(actorId.toString(), "ADMIN_USER_DELETE", "USER", userId.toString(), null, "hard-deleted");
    }

    private UserAccount requireUser(UUID userId) {
        return adminUserPort.findById(userId)
                .orElseThrow(() -> new NotFoundException("user not found"));
    }

    private AdminUserSummaryView toView(UserAccount user) {
        return new AdminUserSummaryView(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getFullName(),
                user.getRole(),
                user.getStatus()
        );
    }
}
