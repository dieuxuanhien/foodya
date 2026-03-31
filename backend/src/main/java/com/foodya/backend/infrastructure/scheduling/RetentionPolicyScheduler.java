package com.foodya.backend.infrastructure.scheduling;

import com.foodya.backend.application.usecases.RetentionPolicyEnforcementService;
import com.foodya.backend.application.usecases.RetentionPolicyEnforcementService.RetentionCleanupResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RetentionPolicyScheduler {

    private static final Logger log = LoggerFactory.getLogger(RetentionPolicyScheduler.class);

    private final RetentionPolicyEnforcementService retentionPolicyEnforcementService;

    public RetentionPolicyScheduler(RetentionPolicyEnforcementService retentionPolicyEnforcementService) {
        this.retentionPolicyEnforcementService = retentionPolicyEnforcementService;
    }

    @Scheduled(cron = "${foodya.jobs.retention.cron:0 30 2 * * *}")
    public void purgeExpiredData() {
        RetentionCleanupResult result = retentionPolicyEnforcementService.enforceRetentionPolicies();
        log.info(
                "Retention cleanup completed: deletedAuditLogs={}, deletedTrackingPoints={}, deletedAiChats={}, auditCutoff={}, trackingCutoff={}, aiChatCutoff={}",
                result.deletedAuditLogs(),
                result.deletedTrackingPoints(),
                result.deletedAiChats(),
                result.auditCutoff(),
                result.trackingCutoff(),
                result.aiChatCutoff()
        );
    }
}
