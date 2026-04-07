package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.dto.NotificationLogData;
import com.foodya.backend.application.ports.out.NotificationLogPort;
import com.foodya.backend.domain.entities.NotificationLog;
import com.foodya.backend.infrastructure.mapper.NotificationLogMapper;
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
public class NotificationLogAdapter implements NotificationLogPort {

    private final NotificationLogRepository notificationLogRepository;
    private final NotificationLogMapper notificationLogMapper;

    public NotificationLogAdapter(NotificationLogRepository notificationLogRepository,
                                  NotificationLogMapper notificationLogMapper) {
        this.notificationLogRepository = notificationLogRepository;
        this.notificationLogMapper = notificationLogMapper;
    }

    @Override
    public NotificationLogData save(NotificationLogData notificationLog) {
        NotificationLog saved = notificationLogMapper.toDomain(
            notificationLogRepository.save(
                Objects.requireNonNull(notificationLogMapper.toPersistence(Objects.requireNonNull(toEntity(Objects.requireNonNull(notificationLog)))))
            )
        );
        return toData(saved);
    }

    @Override
    public PaginatedResult<NotificationLogData> list(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NotificationLog> result = notificationLogRepository.findAll(pageable).map(notificationLogMapper::toDomain);

        return new PaginatedResult<>(
                result.getContent().stream().map(this::toData).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    public PaginatedResult<NotificationLogData> listByReceiver(UUID receiverUserId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NotificationLog> result = notificationLogRepository.findByReceiverUserId(receiverUserId, pageable)
            .map(notificationLogMapper::toDomain);
        return new PaginatedResult<>(
                result.getContent().stream().map(this::toData).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    public Optional<NotificationLogData> markAsRead(UUID receiverUserId, UUID notificationId, OffsetDateTime readAt) {
        return notificationLogRepository.findByIdAndReceiverUserId(notificationId, receiverUserId)
                .map(entity -> {
                    entity.setReadAt(readAt);
                    return toData(notificationLogMapper.toDomain(notificationLogRepository.save(entity)));
                });
    }

    private NotificationLog toEntity(NotificationLogData model) {
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
        entity.setId(model.getId());
        entity.setCreatedAt(model.getCreatedAt());
        return entity;
    }

    private NotificationLogData toData(NotificationLog entity) {
        NotificationLogData data = new NotificationLogData();
        data.setId(entity.getId());
        data.setReceiverUserId(entity.getReceiverUserId());
        data.setReceiverType(entity.getReceiverType());
        data.setEventType(entity.getEventType());
        data.setTitle(entity.getTitle());
        data.setMessage(entity.getMessage());
        data.setStatus(entity.getStatus());
        data.setOrderId(entity.getOrderId());
        data.setProviderResponse(entity.getProviderResponse());
        data.setSentAt(entity.getSentAt());
        data.setReadAt(entity.getReadAt());
        data.setCreatedAt(entity.getCreatedAt());
        return data;
    }
}
