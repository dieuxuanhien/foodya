package com.foodya.backend.application.usecases;

import com.foodya.backend.application.ports.out.SystemParameterPort;
import com.foodya.backend.application.usecases.policy.BackupPolicyEnforcementService;
import com.foodya.backend.domain.entities.SystemParameter;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BackupPolicyEnforcementServiceTests {

    @Test
    void verifyBackupObjectivesAcceptsValidTargets() {
        SystemParameterPort systemParameterPort = mock(SystemParameterPort.class);
        when(systemParameterPort.findById("ops.backup.rpo_minutes")).thenReturn(Optional.of(numberParam("15")));
        when(systemParameterPort.findById("ops.backup.rto_minutes")).thenReturn(Optional.of(numberParam("60")));

        BackupPolicyEnforcementService service = new BackupPolicyEnforcementService(systemParameterPort);
        var status = service.verifyBackupObjectives();

        assertEquals(15, status.rpoMinutes());
        assertEquals(60, status.rtoMinutes());
    }

    @Test
    void verifyBackupObjectivesRejectsOutOfPolicyValues() {
        SystemParameterPort systemParameterPort = mock(SystemParameterPort.class);
        when(systemParameterPort.findById("ops.backup.rpo_minutes")).thenReturn(Optional.of(numberParam("20")));
        when(systemParameterPort.findById("ops.backup.rto_minutes")).thenReturn(Optional.of(numberParam("80")));

        BackupPolicyEnforcementService service = new BackupPolicyEnforcementService(systemParameterPort);
        assertThrows(IllegalStateException.class, service::verifyBackupObjectives);
    }

    private static SystemParameter numberParam(String value) {
        SystemParameter parameter = new SystemParameter();
        parameter.setValue(value);
        return parameter;
    }
}
