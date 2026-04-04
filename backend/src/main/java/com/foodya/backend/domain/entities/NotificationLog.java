package com.foodya.backend.domain.entities;

import com.foodya.backend.domain.value_objects.NotificationReceiverType;
import com.foodya.backend.domain.value_objects.NotificationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public class NotificationLog {

    private UUID id;

    private UUID receiverUserId;

    private NotificationReceiverType receiverType;

    private String eventType;

    private String title;

    private String message;

    private NotificationStatus status;

    private UUID orderId;

    private String providerResponse;

    private OffsetDateTime sentAt;

    private OffsetDateTime readAt;

    private OffsetDateTime createdAt;

    public void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = NotificationStatus.PENDING;
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getReceiverUserId() {
        return receiverUserId;
    }

    public void setReceiverUserId(UUID receiverUserId) {
        this.receiverUserId = receiverUserId;
    }

    public NotificationReceiverType getReceiverType() {
        return receiverType;
    }

    public void setReceiverType(NotificationReceiverType receiverType) {
        this.receiverType = receiverType;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public String getProviderResponse() {
        return providerResponse;
    }

    public void setProviderResponse(String providerResponse) {
        this.providerResponse = providerResponse;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(OffsetDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(OffsetDateTime readAt) {
        this.readAt = readAt;
    }
}
