package com.foodya.backend.infrastructure.scheduling;

import com.foodya.backend.application.ports.in.BackupPolicyEnforcementUseCase;
import com.foodya.backend.application.ports.in.BackupPolicyEnforcementUseCase.BackupPolicyStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BackupPolicyScheduler {

    private static final Logger log = LoggerFactory.getLogger(BackupPolicyScheduler.class);

    private final BackupPolicyEnforcementUseCase backupPolicyEnforcementUseCase;

    public BackupPolicyScheduler(BackupPolicyEnforcementUseCase backupPolicyEnforcementUseCase) {
        this.backupPolicyEnforcementUseCase = backupPolicyEnforcementUseCase;
    }

    @Scheduled(cron = "${foodya.jobs.backup-policy.cron:0 0 */6 * * *}")
    public void enforceBackupPolicy() {
        BackupPolicyStatus status = backupPolicyEnforcementUseCase.verifyBackupObjectives();
        log.info("Backup policy check passed: rpoMinutes={}, rtoMinutes={}", status.rpoMinutes(), status.rtoMinutes());
    }
}
