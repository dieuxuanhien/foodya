package com.foodya.backend.application.usecases;

import com.foodya.backend.application.dto.NotificationLogView;
import com.foodya.backend.application.dto.NotificationLogData;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.dto.DeviceTokenView;
import com.foodya.backend.application.dto.RegisterDeviceTokenCommand;
import com.foodya.backend.application.exception.NotFoundException;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.ports.in.NotificationUseCase;
import com.foodya.backend.application.ports.out.DeviceTokenPort;
import com.foodya.backend.application.ports.out.NotificationLogPort;
import com.foodya.backend.application.ports.out.PushNotificationPort;
import com.foodya.backend.application.support.PaginationPolicy;
import com.foodya.backend.domain.entities.DeviceToken;
import com.foodya.backend.domain.value_objects.DevicePlatform;
import com.foodya.backend.domain.value_objects.NotificationReceiverType;
import com.foodya.backend.domain.value_objects.NotificationStatus;
import com.foodya.backend.domain.value_objects.UserRole;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public class NotificationService implements NotificationUseCase {

    private final NotificationLogPort notificationLogPort;
    private final PushNotificationPort pushNotificationPort;
    private final DeviceTokenPort deviceTokenPort;
    private final PaginationPolicy paginationPolicy;

    public NotificationService(NotificationLogPort notificationLogPort,
                               PushNotificationPort pushNotificationPort,
                               DeviceTokenPort deviceTokenPort,
                               PaginationPolicy paginationPolicy) {
        this.notificationLogPort = notificationLogPort;
        this.pushNotificationPort = pushNotificationPort;
        this.deviceTokenPort = deviceTokenPort;
        this.paginationPolicy = paginationPolicy;
    }

    public NotificationLogView notifyUser(UUID receiverUserId,
                                          UserRole receiverRole,
                                          String eventType,
                                          String title,
                                          String message,
                                          UUID orderId) {
        NotificationLogData log = new NotificationLogData();
        log.setReceiverUserId(receiverUserId);
        log.setReceiverType(mapRole(receiverRole));
        log.setEventType(eventType);
        log.setTitle(title);
        log.setMessage(message);
        log.setOrderId(orderId);

        PushNotificationPort.DeliveryResult result = pushNotificationPort.sendToUser(receiverUserId, title, message, orderId, eventType);
        if (result.delivered()) {
            log.setStatus(NotificationStatus.SENT);
            log.setSentAt(OffsetDateTime.now());
        } else {
            log.setStatus(NotificationStatus.SKIPPED);
        }
        log.setProviderResponse(result.providerResponse());

        NotificationLogData saved = notificationLogPort.save(log);
        return toView(saved);
    }

    public PaginatedResult<NotificationLogView> list(Integer page, Integer size) {
        PaginationPolicy.PaginationSpec spec = paginationPolicy.page(page, size);
        PaginatedResult<NotificationLogData> result = notificationLogPort.list(spec.page(), spec.size());

        return new PaginatedResult<>(
                result.items().stream().map(this::toView).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    public PaginatedResult<NotificationLogView> listForUser(UUID receiverUserId, Integer page, Integer size) {
        PaginationPolicy.PaginationSpec spec = paginationPolicy.page(page, size);
        PaginatedResult<NotificationLogData> result = notificationLogPort.listByReceiver(receiverUserId, spec.page(), spec.size());

        return new PaginatedResult<>(
                result.items().stream().map(this::toView).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    public NotificationLogView markAsRead(UUID receiverUserId, UUID notificationId) {
        NotificationLogData model = notificationLogPort.markAsRead(receiverUserId, notificationId, OffsetDateTime.now())
                .orElseThrow(() -> new NotFoundException("notification not found"));
        return toView(model);
    }

    public DeviceTokenView registerDevice(UUID userId, RegisterDeviceTokenCommand command) {
        String token = normalizeToken(command.token());
        DevicePlatform platform = command.platform() == null ? DevicePlatform.UNKNOWN : command.platform();

        DeviceToken deviceToken = deviceTokenPort.findByToken(token).orElseGet(DeviceToken::new);
        deviceToken.setUserId(userId);
        deviceToken.setToken(token);
        deviceToken.setPlatform(platform);
        deviceToken.setDeviceId(trimToNull(command.deviceId()));
        deviceToken.setAppVersion(trimToNull(command.appVersion()));
        deviceToken.touch();

        return toDeviceTokenView(deviceTokenPort.save(deviceToken));
    }

    public void unregisterDevice(UUID userId, String token) {
        deviceTokenPort.disableToken(userId, normalizeToken(token));
    }

    private String normalizeToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ValidationException("device token is required", Map.of("token", "Device token is required."));
        }
        String normalized = token.trim();
        if (normalized.length() > 4096) {
            throw new ValidationException("device token is too long", Map.of("token", "Device token must be 4096 characters or fewer."));
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private NotificationReceiverType mapRole(UserRole role) {
        return switch (role) {
            case CUSTOMER -> NotificationReceiverType.CUSTOMER;
            case MERCHANT -> NotificationReceiverType.MERCHANT;
            case DELIVERY -> NotificationReceiverType.DELIVERY;
            case ADMIN -> NotificationReceiverType.ADMIN;
        };
    }

    private NotificationLogView toView(NotificationLogData log) {
        return new NotificationLogView(
                log.getId(),
                log.getReceiverUserId(),
                log.getReceiverType(),
                log.getEventType(),
                log.getTitle(),
                log.getMessage(),
                log.getStatus(),
                log.getOrderId(),
                log.getSentAt(),
                log.getReadAt(),
                log.getCreatedAt()
        );
    }

    private DeviceTokenView toDeviceTokenView(DeviceToken deviceToken) {
        return new DeviceTokenView(
                deviceToken.getId(),
                deviceToken.getUserId(),
                deviceToken.getPlatform(),
                deviceToken.getDeviceId(),
                deviceToken.getAppVersion(),
                deviceToken.isEnabled(),
                deviceToken.getLastSeenAt(),
                deviceToken.getCreatedAt(),
                deviceToken.getUpdatedAt()
        );
    }
}
