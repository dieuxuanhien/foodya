package com.foodya.backend.application.ports.in;

public interface BackupPolicyEnforcementUseCase {

    BackupPolicyStatus verifyBackupObjectives();

    record BackupPolicyStatus(int rpoMinutes, int rtoMinutes) {
    }
}
