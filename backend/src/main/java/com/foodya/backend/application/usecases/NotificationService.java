package com.foodya.backend.application.usecases;

import com.foodya.backend.application.dto.NotificationLogView;
import com.foodya.backend.application.dto.NotificationLogModel;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.exception.NotFoundException;
import com.foodya.backend.application.ports.out.NotificationLogPort;
import com.foodya.backend.application.ports.out.PushNotificationPort;
import com.foodya.backend.domain.value_objects.NotificationReceiverType;
import com.foodya.backend.domain.value_objects.NotificationStatus;
import com.foodya.backend.domain.value_objects.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationLogPort notificationLogPort;
    private final PushNotificationPort pushNotificationPort;
    private final PaginationPolicyService paginationPolicyService;

    public NotificationService(NotificationLogPort notificationLogPort,
                               PushNotificationPort pushNotificationPort,
                               PaginationPolicyService paginationPolicyService) {
        this.notificationLogPort = notificationLogPort;
        this.pushNotificationPort = pushNotificationPort;
        this.paginationPolicyService = paginationPolicyService;
    }

    @Transactional
    public NotificationLogView notifyUser(UUID receiverUserId,
                                          UserRole receiverRole,
                                          String eventType,
                                          String title,
                                          String message,
                                          UUID orderId) {
        NotificationLogModel log = new NotificationLogModel();
        log.setReceiverUserId(receiverUserId);
        log.setReceiverType(mapRole(receiverRole));
        log.setEventType(eventType);
        log.setTitle(title);
        log.setMessage(message);
        log.setOrderId(orderId);

        PushNotificationPort.DeliveryResult result = pushNotificationPort.sendToUser(receiverUserId, title, message);
        if (result.delivered()) {
            log.setStatus(NotificationStatus.SENT);
            log.setSentAt(OffsetDateTime.now());
        } else {
            log.setStatus(NotificationStatus.SKIPPED);
        }
        log.setProviderResponse(result.providerResponse());

        NotificationLogModel saved = notificationLogPort.save(log);
        return toView(saved);
    }

    @Transactional(readOnly = true)
    public PaginatedResult<NotificationLogView> list(Integer page, Integer size) {
        PaginationPolicyService.PaginationSpec spec = paginationPolicyService.page(page, size);
        PaginatedResult<NotificationLogModel> result = notificationLogPort.list(spec.page(), spec.size());

        return new PaginatedResult<>(
                result.items().stream().map(this::toView).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    @Transactional(readOnly = true)
    public PaginatedResult<NotificationLogView> listForUser(UUID receiverUserId, Integer page, Integer size) {
        PaginationPolicyService.PaginationSpec spec = paginationPolicyService.page(page, size);
        PaginatedResult<NotificationLogModel> result = notificationLogPort.listByReceiver(receiverUserId, spec.page(), spec.size());

        return new PaginatedResult<>(
                result.items().stream().map(this::toView).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    @Transactional
    public NotificationLogView markAsRead(UUID receiverUserId, UUID notificationId) {
        NotificationLogModel model = notificationLogPort.markAsRead(receiverUserId, notificationId, OffsetDateTime.now())
                .orElseThrow(() -> new NotFoundException("notification not found"));
        return toView(model);
    }

    private NotificationReceiverType mapRole(UserRole role) {
        return switch (role) {
            case CUSTOMER -> NotificationReceiverType.CUSTOMER;
            case MERCHANT -> NotificationReceiverType.MERCHANT;
            case DELIVERY -> NotificationReceiverType.DELIVERY;
            case ADMIN -> NotificationReceiverType.ADMIN;
        };
    }

    private NotificationLogView toView(NotificationLogModel log) {
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
}
