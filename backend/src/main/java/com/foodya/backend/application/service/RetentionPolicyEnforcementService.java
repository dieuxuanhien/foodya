package com.foodya.backend.application.service;

import com.foodya.backend.application.port.out.AiChatHistoryPort;
import com.foodya.backend.application.port.out.AuditLogPort;
import com.foodya.backend.application.port.out.DeliveryTrackingPointPort;
import com.foodya.backend.application.port.out.SystemParameterPort;
import com.foodya.backend.domain.persistence.SystemParameter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class RetentionPolicyEnforcementService {

    private static final String RETENTION_AUDIT_LOGS_DAYS = "retention.audit_logs_days";
    private static final String RETENTION_TRACKING_POINTS_DAYS = "retention.tracking_points_days";
    private static final String RETENTION_AI_CHAT_DAYS = "retention.ai_chat_days";

    private final SystemParameterPort systemParameterPort;
    private final AuditLogPort auditLogPort;
    private final DeliveryTrackingPointPort deliveryTrackingPointPort;
    private final AiChatHistoryPort aiChatHistoryPort;

    public RetentionPolicyEnforcementService(SystemParameterPort systemParameterPort,
                                             AuditLogPort auditLogPort,
                                             DeliveryTrackingPointPort deliveryTrackingPointPort,
                                             AiChatHistoryPort aiChatHistoryPort) {
        this.systemParameterPort = systemParameterPort;
        this.auditLogPort = auditLogPort;
        this.deliveryTrackingPointPort = deliveryTrackingPointPort;
        this.aiChatHistoryPort = aiChatHistoryPort;
    }

    @Transactional
    public RetentionCleanupResult enforceRetentionPolicies() {
        return enforceRetentionPolicies(OffsetDateTime.now());
    }

    @Transactional
    public RetentionCleanupResult enforceRetentionPolicies(OffsetDateTime now) {
        int auditLogsDays = intParam(RETENTION_AUDIT_LOGS_DAYS, 365);
        int trackingPointsDays = intParam(RETENTION_TRACKING_POINTS_DAYS, 30);
        int aiChatDays = intParam(RETENTION_AI_CHAT_DAYS, 90);

        OffsetDateTime auditCutoff = now.minusDays(auditLogsDays);
        OffsetDateTime trackingCutoff = now.minusDays(trackingPointsDays);
        OffsetDateTime aiChatCutoff = now.minusDays(aiChatDays);

        long deletedAuditLogs = auditLogPort.deleteByCreatedAtBefore(auditCutoff);
        long deletedTrackingPoints = deliveryTrackingPointPort.deleteByRecordedAtBefore(trackingCutoff);
        long deletedAiChats = aiChatHistoryPort.deleteByCreatedAtBefore(aiChatCutoff);

        return new RetentionCleanupResult(
                deletedAuditLogs,
                deletedTrackingPoints,
                deletedAiChats,
                auditCutoff,
                trackingCutoff,
                aiChatCutoff
        );
    }

    private int intParam(String key, int fallback) {
        return systemParameterPort.findById(key)
                .map(SystemParameter::getValue)
                .map(String::trim)
                .map(value -> {
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException ex) {
                        return fallback;
                    }
                })
                .filter(v -> v > 0)
                .orElse(fallback);
    }

    public record RetentionCleanupResult(
            long deletedAuditLogs,
            long deletedTrackingPoints,
            long deletedAiChats,
            OffsetDateTime auditCutoff,
            OffsetDateTime trackingCutoff,
            OffsetDateTime aiChatCutoff
    ) {
    }
}
