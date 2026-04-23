package com.foodya.backend.domain.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

public class AuditLog {

    private UUID id;

    private String actorUserId;

    private String action;

    private String targetType;

    private String targetId;

    private String oldValue;

    private String newValue;

    private OffsetDateTime createdAt;

    public void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public static AuditLog parameterUpdate(String actor, String targetId, String oldValue, String newValue) {
        AuditLog log = new AuditLog();
        log.actorUserId = actor;
        log.action = "SYSTEM_PARAMETER_UPDATED";
        log.targetType = "SYSTEM_PARAMETER";
        log.targetId = targetId;
        log.oldValue = oldValue;
        log.newValue = newValue;
        return log;
    }

    public static AuditLog securityEvent(String actor, String action, String targetType, String targetId, String oldValue, String newValue) {
        AuditLog log = new AuditLog();
        log.actorUserId = actor;
        log.action = action;
        log.targetType = targetType;
        log.targetId = targetId;
        log.oldValue = oldValue;
        log.newValue = newValue;
        return log;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(String actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
