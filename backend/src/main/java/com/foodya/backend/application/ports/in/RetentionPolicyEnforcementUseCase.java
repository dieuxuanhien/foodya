package com.foodya.backend.application.ports.in;

import java.time.OffsetDateTime;

public interface RetentionPolicyEnforcementUseCase {

    RetentionCleanupResult enforceRetentionPolicies();

    RetentionCleanupResult enforceRetentionPolicies(OffsetDateTime now);

    record RetentionCleanupResult(
            long deletedAuditLogs,
            long deletedTrackingPoints,
            long deletedAiChats,
            OffsetDateTime auditCutoff,
            OffsetDateTime trackingCutoff,
            OffsetDateTime aiChatCutoff
    ) {
    }
}
