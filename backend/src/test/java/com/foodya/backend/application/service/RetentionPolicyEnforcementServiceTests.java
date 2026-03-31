package com.foodya.backend.application.service;

import com.foodya.backend.application.port.out.AiChatHistoryPort;
import com.foodya.backend.application.port.out.AuditLogPort;
import com.foodya.backend.application.port.out.DeliveryTrackingPointPort;
import com.foodya.backend.application.port.out.SystemParameterPort;
import com.foodya.backend.domain.model.ParameterValueType;
import com.foodya.backend.domain.persistence.SystemParameter;
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
        RetentionPolicyEnforcementService.RetentionCleanupResult result = service.enforceRetentionPolicies(now);

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
        RetentionPolicyEnforcementService.RetentionCleanupResult result = service.enforceRetentionPolicies(now);

        assertEquals(now.minusDays(365), result.auditCutoff());
        assertEquals(now.minusDays(30), result.trackingCutoff());
        assertEquals(now.minusDays(90), result.aiChatCutoff());
    }

    private static SystemParameter numberParam(String value) {
        SystemParameter parameter = new SystemParameter();
        parameter.setValueType(ParameterValueType.NUMBER);
        parameter.setValue(value);
        return parameter;
    }
}
