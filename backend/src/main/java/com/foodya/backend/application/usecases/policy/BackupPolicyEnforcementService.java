package com.foodya.backend.application.usecases.policy;

import com.foodya.backend.application.ports.out.SystemParameterPort;
import com.foodya.backend.domain.entities.SystemParameter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BackupPolicyEnforcementService implements com.foodya.backend.application.ports.in.BackupPolicyEnforcementUseCase {

    private static final String RPO_KEY = "ops.backup.rpo_minutes";
    private static final String RTO_KEY = "ops.backup.rto_minutes";

    private final SystemParameterPort systemParameterPort;

    public BackupPolicyEnforcementService(SystemParameterPort systemParameterPort) {
        this.systemParameterPort = systemParameterPort;
    }

    @Transactional(readOnly = true)
    @Override
    public BackupPolicyStatus verifyBackupObjectives() {
        int rpoMinutes = intParam(RPO_KEY, 15);
        int rtoMinutes = intParam(RTO_KEY, 60);

        if (rpoMinutes <= 0 || rpoMinutes > 15) {
            throw new IllegalStateException("backup RPO policy violation: ops.backup.rpo_minutes must be between 1 and 15");
        }
        if (rtoMinutes <= 0 || rtoMinutes > 60) {
            throw new IllegalStateException("backup RTO policy violation: ops.backup.rto_minutes must be between 1 and 60");
        }

        return new BackupPolicyStatus(rpoMinutes, rtoMinutes);
    }

    private int intParam(String key, int fallback) {
        return systemParameterPort.findById(key)
                .map(SystemParameter::getValue)
                .map(String::trim)
                .map(Integer::parseInt)
                .orElse(fallback);
    }
}
