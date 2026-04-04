package com.foodya.backend.application.usecases.policy;

import com.foodya.backend.application.ports.out.AiChatHistoryPort;
import com.foodya.backend.application.ports.out.AuditLogPort;
import com.foodya.backend.application.ports.out.DeliveryTrackingPointPort;
import com.foodya.backend.application.ports.out.SystemParameterPort;
import com.foodya.backend.domain.entities.SystemParameter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class RetentionPolicyEnforcementService implements com.foodya.backend.application.ports.in.RetentionPolicyEnforcementUseCase {

    private static final String RETENTION_CUSTOMER_DATA_DAYS = "retention.customer_data_days";
    private static final String RETENTION_ORDER_HISTORY_DAYS = "retention.order_history_days";
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
    @Override
    public RetentionCleanupResult enforceRetentionPolicies() {
        return enforceRetentionPolicies(OffsetDateTime.now());
    }

    @Transactional
    @Override
    public RetentionCleanupResult enforceRetentionPolicies(OffsetDateTime now) {
        int auditLogsDays = intParamWithFallback(RETENTION_AUDIT_LOGS_DAYS, RETENTION_CUSTOMER_DATA_DAYS, 365);
        int trackingPointsDays = intParamWithFallback(RETENTION_TRACKING_POINTS_DAYS, RETENTION_ORDER_HISTORY_DAYS, 30);
        int aiChatDays = intParamWithFallback(RETENTION_AI_CHAT_DAYS, RETENTION_CUSTOMER_DATA_DAYS, 90);

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

    private int intParamWithFallback(String primaryKey, String fallbackKey, int defaultValue) {
        return intParam(primaryKey)
                .or(() -> intParam(fallbackKey))
                .orElse(defaultValue);
    }

    private java.util.Optional<Integer> intParam(String key) {
        return systemParameterPort.findById(key)
                .map(SystemParameter::getValue)
                .map(String::trim)
                .map(value -> {
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                })
                .filter(v -> v != null && v > 0);
    }
}
