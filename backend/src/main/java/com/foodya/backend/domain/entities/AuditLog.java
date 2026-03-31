package com.foodya.backend.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "actor_user_id", length = 128)
    private String actorUserId;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "target_type", nullable = false, length = 64)
    private String targetType;

    @Column(name = "target_id", nullable = false, length = 128)
    private String targetId;

    @Column(name = "old_value", columnDefinition = "text")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "text")
    private String newValue;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
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
}
