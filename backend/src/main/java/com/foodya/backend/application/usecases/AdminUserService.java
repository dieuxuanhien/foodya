package com.foodya.backend.application.usecases;

import com.foodya.backend.application.dto.AdminUserSummaryView;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.exception.ConflictException;
import com.foodya.backend.application.exception.NotFoundException;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.ports.in.AdminUserUseCase;
import com.foodya.backend.application.ports.out.AdminUserPort;
import com.foodya.backend.application.support.PaginationPolicy;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.value_objects.UserStatus;
import com.foodya.backend.domain.entities.UserAccount;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    private final com.foodya.backend.application.ports.out.PasswordHashPort passwordHashPort;

    public AdminUserService(AdminUserPort adminUserPort,
                            PaginationPolicy paginationPolicy,
                            AuditLogService auditLogService,
                            com.foodya.backend.application.ports.out.PasswordHashPort passwordHashPort) {
        this.adminUserPort = adminUserPort;
        this.paginationPolicy = paginationPolicy;
        this.auditLogService = auditLogService;
        this.passwordHashPort = passwordHashPort;
    }

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

    public AdminUserSummaryView approve(UUID userId, UUID actorId) {
        UserAccount user = requireUser(userId);
        UserStatus oldStatus = user.getStatus();

        if (oldStatus != UserStatus.PENDING_APPROVAL) {
            throw new ValidationException("cannot approve", Map.of("status", "only PENDING_APPROVAL users can be approved"));
        }

        user.setStatus(UserStatus.ACTIVE);
        UserAccount updated = adminUserPort.save(user);

        auditLogService.securityEvent(
                actorId.toString(),
                "ADMIN_USER_APPROVE",
                "USER",
                userId.toString(),
                oldStatus.name(),
                updated.getStatus().name()
        );

        return toView(updated);
    }

    public void delete(UUID userId, UUID actorId) {
        UserAccount user = requireUser(userId);

        boolean hasCustomerOrders = adminUserPort.hasCustomerOrdersInStatuses(userId, BLOCKING_DELETE_STATUSES);
        boolean hasMerchantOrders = adminUserPort.hasMerchantOrdersInStatuses(userId, BLOCKING_DELETE_STATUSES);
        if (hasCustomerOrders || hasMerchantOrders) {
            throw new ConflictException("soft delete blocked by linked active orders");
        }

        user.setDeletedAt(OffsetDateTime.now());
        adminUserPort.save(user);
        auditLogService.securityEvent(actorId.toString(), "ADMIN_USER_DELETE", "USER", userId.toString(), null, "soft-deleted");
    }

    public com.foodya.backend.application.dto.AdminUserDetailView getById(UUID userId) {
        UserAccount user = requireUser(userId);
        return new com.foodya.backend.application.dto.AdminUserDetailView(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public AdminUserSummaryView createUser(com.foodya.backend.application.dto.AdminUserCreateCommand command, UUID actorId) {
        if (adminUserPort.findByUsername(command.username()).isPresent()) {
            throw new ConflictException("Username already exists");
        }
        if (adminUserPort.findByEmail(command.email()).isPresent()) {
            throw new ConflictException("Email already exists");
        }
        if (adminUserPort.findByPhoneNumber(command.phoneNumber()).isPresent()) {
            throw new ConflictException("Phone number already exists");
        }

        UserAccount user = new UserAccount();
        user.onCreate();
        user.setUsername(command.username());
        user.setEmail(command.email());
        user.setPhoneNumber(command.phoneNumber());
        user.setFullName(command.fullName());
        user.setRole(command.role());
        user.setStatus(command.status());
        user.setPasswordHash(passwordHashPort.encode(command.password()));

        UserAccount saved = adminUserPort.save(user);
        auditLogService.securityEvent(actorId.toString(), "ADMIN_USER_CREATE", "USER", saved.getId().toString(), null, saved.getStatus().name());
        return toView(saved);
    }

    public AdminUserSummaryView updateUser(UUID userId, com.foodya.backend.application.dto.AdminUserUpdateCommand command, UUID actorId) {
        UserAccount user = requireUser(userId);

        if (!user.getUsername().equals(command.username()) && adminUserPort.findByUsername(command.username()).isPresent()) {
            throw new ConflictException("Username already exists");
        }
        if (!user.getEmail().equals(command.email()) && adminUserPort.findByEmail(command.email()).isPresent()) {
            throw new ConflictException("Email already exists");
        }
        if (!user.getPhoneNumber().equals(command.phoneNumber()) && adminUserPort.findByPhoneNumber(command.phoneNumber()).isPresent()) {
            throw new ConflictException("Phone number already exists");
        }

        user.setUsername(command.username());
        user.setEmail(command.email());
        user.setPhoneNumber(command.phoneNumber());
        user.setFullName(command.fullName());
        user.setRole(command.role());
        user.setStatus(command.status());
        user.onUpdate();

        UserAccount saved = adminUserPort.save(user);
        auditLogService.securityEvent(actorId.toString(), "ADMIN_USER_UPDATE", "USER", saved.getId().toString(), null, "Updated");
        return toView(saved);
    }

    public void resetPassword(UUID userId, String newPassword, UUID actorId) {
        UserAccount user = requireUser(userId);
        user.setPasswordHash(passwordHashPort.encode(newPassword));
        user.onUpdate();
        adminUserPort.save(user);
        auditLogService.securityEvent(actorId.toString(), "ADMIN_USER_RESET_PASSWORD", "USER", userId.toString(), null, "Password reset");
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
