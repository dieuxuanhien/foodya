package com.foodya.backend.application.usecases;

import com.foodya.backend.application.ports.out.AiChatHistoryPort;
import com.foodya.backend.application.ports.out.AuditLogPort;
import com.foodya.backend.application.ports.out.DeliveryTrackingPointPort;
import com.foodya.backend.application.ports.out.SystemParameterPort;
import com.foodya.backend.application.usecases.policy.RetentionPolicyEnforcementService;
import com.foodya.backend.domain.value_objects.ParameterValueType;
import com.foodya.backend.domain.entities.SystemParameter;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RetentionPolicyEnforcementServiceTests {

    @Test
    void enforceRetentionPoliciesUsesConfiguredDays() {
        SystemParameterPort systemParameterPort = mock(SystemParameterPort.class);
        AuditLogPort auditLogPort = mock(AuditLogPort.class);
        DeliveryTrackingPointPort trackingPointPort = mock(DeliveryTrackingPointPort.class);
        AiChatHistoryPort aiChatHistoryPort = mock(AiChatHistoryPort.class);

        when(systemParameterPort.findById("retention.audit_logs_days")).thenReturn(Optional.of(numberParam("7")));
        when(systemParameterPort.findById("retention.tracking_points_days")).thenReturn(Optional.of(numberParam("10")));
        when(systemParameterPort.findById("retention.ai_chat_days")).thenReturn(Optional.of(numberParam("15")));

        when(auditLogPort.deleteByCreatedAtBefore(any())).thenReturn(11L);
        when(trackingPointPort.deleteByRecordedAtBefore(any())).thenReturn(22L);
        when(aiChatHistoryPort.deleteByCreatedAtBefore(any())).thenReturn(33L);

        RetentionPolicyEnforcementService service = new RetentionPolicyEnforcementService(
                systemParameterPort,
                auditLogPort,
                trackingPointPort,
                aiChatHistoryPort
        );

        OffsetDateTime now = OffsetDateTime.parse("2026-03-31T00:00:00Z");
        var result = service.enforceRetentionPolicies(now);

        assertEquals(11L, result.deletedAuditLogs());
        assertEquals(22L, result.deletedTrackingPoints());
        assertEquals(33L, result.deletedAiChats());
        assertEquals(now.minusDays(7), result.auditCutoff());
        assertEquals(now.minusDays(10), result.trackingCutoff());
        assertEquals(now.minusDays(15), result.aiChatCutoff());

        verify(auditLogPort).deleteByCreatedAtBefore(now.minusDays(7));
        verify(trackingPointPort).deleteByRecordedAtBefore(now.minusDays(10));
        verify(aiChatHistoryPort).deleteByCreatedAtBefore(now.minusDays(15));
    }

    @Test
    void enforceRetentionPoliciesFallsBackToDefaultsOnInvalidValues() {
        SystemParameterPort systemParameterPort = mock(SystemParameterPort.class);
        AuditLogPort auditLogPort = mock(AuditLogPort.class);
        DeliveryTrackingPointPort trackingPointPort = mock(DeliveryTrackingPointPort.class);
        AiChatHistoryPort aiChatHistoryPort = mock(AiChatHistoryPort.class);

        when(systemParameterPort.findById("retention.audit_logs_days")).thenReturn(Optional.of(numberParam("invalid")));
        when(systemParameterPort.findById("retention.tracking_points_days")).thenReturn(Optional.of(numberParam("-1")));
        when(systemParameterPort.findById("retention.customer_data_days")).thenReturn(Optional.empty());
        when(systemParameterPort.findById("retention.order_history_days")).thenReturn(Optional.empty());
        when(systemParameterPort.findById("retention.ai_chat_days")).thenReturn(Optional.empty());

        when(auditLogPort.deleteByCreatedAtBefore(any())).thenReturn(1L);
        when(trackingPointPort.deleteByRecordedAtBefore(any())).thenReturn(2L);
        when(aiChatHistoryPort.deleteByCreatedAtBefore(any())).thenReturn(3L);

        RetentionPolicyEnforcementService service = new RetentionPolicyEnforcementService(
                systemParameterPort,
                auditLogPort,
                trackingPointPort,
                aiChatHistoryPort
        );

        OffsetDateTime now = OffsetDateTime.parse("2026-03-31T12:00:00Z");
        var result = service.enforceRetentionPolicies(now);

        assertEquals(now.minusDays(365), result.auditCutoff());
        assertEquals(now.minusDays(30), result.trackingCutoff());
        assertEquals(now.minusDays(90), result.aiChatCutoff());
    }

    @Test
    void enforceRetentionPoliciesUsesLegacyKeysWhenSrsKeysMissing() {
        SystemParameterPort systemParameterPort = mock(SystemParameterPort.class);
        AuditLogPort auditLogPort = mock(AuditLogPort.class);
        DeliveryTrackingPointPort trackingPointPort = mock(DeliveryTrackingPointPort.class);
        AiChatHistoryPort aiChatHistoryPort = mock(AiChatHistoryPort.class);

        when(systemParameterPort.findById("retention.customer_data_days")).thenReturn(Optional.empty());
        when(systemParameterPort.findById("retention.order_history_days")).thenReturn(Optional.empty());
        when(systemParameterPort.findById("retention.audit_logs_days")).thenReturn(Optional.of(numberParam("40")));
        when(systemParameterPort.findById("retention.tracking_points_days")).thenReturn(Optional.of(numberParam("50")));
        when(systemParameterPort.findById("retention.ai_chat_days")).thenReturn(Optional.of(numberParam("60")));

        when(auditLogPort.deleteByCreatedAtBefore(any())).thenReturn(1L);
        when(trackingPointPort.deleteByRecordedAtBefore(any())).thenReturn(2L);
        when(aiChatHistoryPort.deleteByCreatedAtBefore(any())).thenReturn(3L);

        RetentionPolicyEnforcementService service = new RetentionPolicyEnforcementService(
                systemParameterPort,
                auditLogPort,
                trackingPointPort,
                aiChatHistoryPort
        );

        OffsetDateTime now = OffsetDateTime.parse("2026-03-31T12:00:00Z");
        var result = service.enforceRetentionPolicies(now);

        assertEquals(now.minusDays(40), result.auditCutoff());
        assertEquals(now.minusDays(50), result.trackingCutoff());
        assertEquals(now.minusDays(60), result.aiChatCutoff());
    }

    @Test
    void enforceRetentionPoliciesFallsBackAiChatToCustomerDataDaysWhenAiKeyMissing() {
        SystemParameterPort systemParameterPort = mock(SystemParameterPort.class);
        AuditLogPort auditLogPort = mock(AuditLogPort.class);
        DeliveryTrackingPointPort trackingPointPort = mock(DeliveryTrackingPointPort.class);
        AiChatHistoryPort aiChatHistoryPort = mock(AiChatHistoryPort.class);

        when(systemParameterPort.findById("retention.customer_data_days")).thenReturn(Optional.of(numberParam("21")));
        when(systemParameterPort.findById("retention.order_history_days")).thenReturn(Optional.of(numberParam("30")));
        when(systemParameterPort.findById("retention.ai_chat_days")).thenReturn(Optional.empty());

        when(auditLogPort.deleteByCreatedAtBefore(any())).thenReturn(1L);
        when(trackingPointPort.deleteByRecordedAtBefore(any())).thenReturn(2L);
        when(aiChatHistoryPort.deleteByCreatedAtBefore(any())).thenReturn(3L);

        RetentionPolicyEnforcementService service = new RetentionPolicyEnforcementService(
                systemParameterPort,
                auditLogPort,
                trackingPointPort,
                aiChatHistoryPort
        );

        OffsetDateTime now = OffsetDateTime.parse("2026-03-31T12:00:00Z");
        var result = service.enforceRetentionPolicies(now);

        assertEquals(now.minusDays(21), result.aiChatCutoff());
    }

    private static SystemParameter numberParam(String value) {
        SystemParameter parameter = new SystemParameter();
        parameter.setValueType(ParameterValueType.NUMBER);
        parameter.setValue(value);
        return parameter;
    }
}
