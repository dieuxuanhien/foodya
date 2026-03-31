package com.foodya.backend.application.usecases;

import com.foodya.backend.application.ports.out.AiChatHistoryPort;
import com.foodya.backend.application.ports.out.AuditLogPort;
import com.foodya.backend.application.ports.out.DeliveryTrackingPointPort;
import com.foodya.backend.application.ports.out.SystemParameterPort;
import com.foodya.backend.domain.entities.SystemParameter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class RetentionPolicyEnforcementService {

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
    public RetentionCleanupResult enforceRetentionPolicies() {
        return enforceRetentionPolicies(OffsetDateTime.now());
    }

    @Transactional
    public RetentionCleanupResult enforceRetentionPolicies(OffsetDateTime now) {
        int customerDataDays = intParamWithFallback(RETENTION_CUSTOMER_DATA_DAYS, RETENTION_AUDIT_LOGS_DAYS, 365);
        int orderHistoryDays = intParamWithFallback(RETENTION_ORDER_HISTORY_DAYS, RETENTION_TRACKING_POINTS_DAYS, 730);
        int aiChatDays = intParamWithFallback(RETENTION_AI_CHAT_DAYS, RETENTION_CUSTOMER_DATA_DAYS, 365);

        OffsetDateTime auditCutoff = now.minusDays(customerDataDays);
        OffsetDateTime trackingCutoff = now.minusDays(orderHistoryDays);
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
