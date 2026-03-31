package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.dto.NotificationLogModel;
import com.foodya.backend.application.ports.out.NotificationLogPort;
import com.foodya.backend.domain.entities.NotificationLog;
import com.foodya.backend.infrastructure.repository.NotificationLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class NotificationLogPersistenceAdapter implements NotificationLogPort {

    private final NotificationLogRepository notificationLogRepository;

    public NotificationLogPersistenceAdapter(NotificationLogRepository notificationLogRepository) {
        this.notificationLogRepository = notificationLogRepository;
    }

    @Override
    public NotificationLogModel save(NotificationLogModel notificationLog) {
        NotificationLog saved = notificationLogRepository.save(Objects.requireNonNull(toEntity(Objects.requireNonNull(notificationLog))));
        return toModel(saved);
    }

    @Override
    public PaginatedResult<NotificationLogModel> list(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NotificationLog> result = notificationLogRepository.findAll(pageable);

        return new PaginatedResult<>(
                result.getContent().stream().map(this::toModel).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    public PaginatedResult<NotificationLogModel> listByReceiver(UUID receiverUserId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NotificationLog> result = notificationLogRepository.findByReceiverUserId(receiverUserId, pageable);
        return new PaginatedResult<>(
                result.getContent().stream().map(this::toModel).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    public Optional<NotificationLogModel> markAsRead(UUID receiverUserId, UUID notificationId, OffsetDateTime readAt) {
        return notificationLogRepository.findByIdAndReceiverUserId(notificationId, receiverUserId)
                .map(entity -> {
                    entity.setReadAt(readAt);
                    return toModel(notificationLogRepository.save(entity));
                });
    }

    private NotificationLog toEntity(NotificationLogModel model) {
        NotificationLog entity = new NotificationLog();
        entity.setReceiverUserId(model.getReceiverUserId());
        entity.setReceiverType(model.getReceiverType());
        entity.setEventType(model.getEventType());
        entity.setTitle(model.getTitle());
        entity.setMessage(model.getMessage());
        entity.setStatus(model.getStatus());
        entity.setOrderId(model.getOrderId());
        entity.setProviderResponse(model.getProviderResponse());
        entity.setSentAt(model.getSentAt());
        entity.setReadAt(model.getReadAt());
        return entity;
    }

    private NotificationLogModel toModel(NotificationLog entity) {
        NotificationLogModel model = new NotificationLogModel();
        model.setId(entity.getId());
        model.setReceiverUserId(entity.getReceiverUserId());
        model.setReceiverType(entity.getReceiverType());
        model.setEventType(entity.getEventType());
        model.setTitle(entity.getTitle());
        model.setMessage(entity.getMessage());
        model.setStatus(entity.getStatus());
        model.setOrderId(entity.getOrderId());
        model.setProviderResponse(entity.getProviderResponse());
        model.setSentAt(entity.getSentAt());
        model.setReadAt(entity.getReadAt());
        model.setCreatedAt(entity.getCreatedAt());
        return model;
    }
}
