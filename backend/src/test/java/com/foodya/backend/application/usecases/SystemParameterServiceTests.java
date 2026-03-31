package com.foodya.backend.application.usecases;

import com.foodya.backend.domain.value_objects.ParameterValueType;
import com.foodya.backend.application.dto.SystemParameterModel;
import com.foodya.backend.application.dto.SystemParameterPatchRequest;
import com.foodya.backend.application.dto.SystemParameterPutRequest;
import com.foodya.backend.application.exception.ForbiddenException;
import com.foodya.backend.application.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SystemParameterServiceTests {

    @Autowired
    private SystemParameterService systemParameterService;

    @Test
    void bootstrapCreatesDefaultKeys() {
        List<SystemParameterModel> parameters = systemParameterService.listAll();
        assertTrue(parameters.stream().anyMatch(p -> p.getKey().equals("shipping.base_delivery_fee")));
        assertTrue(parameters.stream().anyMatch(p -> p.getKey().equals("ops.backup.rto_minutes")));
    }

    @Test
    void patchUpdatesValueAndVersion() {
        SystemParameterModel before = systemParameterService.listAll().stream()
                .filter(p -> p.getKey().equals("shipping.base_delivery_fee"))
                .findFirst()
                .orElseThrow();
        int beforeVersion = before.getVersion();

        SystemParameterModel updated = systemParameterService.patch(
                "shipping.base_delivery_fee",
                new SystemParameterPatchRequest(null, "12000", null, "Update for test"),
                "ADMIN",
                "admin-user-1"
        );

        assertEquals("12000", updated.getValue());
        assertEquals(beforeVersion + 1, updated.getVersion());
        assertEquals("admin-user-1", updated.getUpdatedByActor());
    }

    @Test
    void patchRequiresAdminRole() {
        assertThrows(ForbiddenException.class, () -> systemParameterService.patch(
                "shipping.base_delivery_fee",
                new SystemParameterPatchRequest(null, "12000", null, null),
                "MERCHANT",
                "merchant-1"
        ));
    }

    @Test
    void replaceRejectsInvalidRange() {
        assertThrows(ValidationException.class, () -> systemParameterService.replace(
                "shipping.base_delivery_fee",
                new SystemParameterPutRequest(ParameterValueType.NUMBER, "-1", true, "invalid"),
                "ADMIN",
                "admin-user-1"
        ));
    }

    @Test
    void patchRejectsRuntimeApplicableMismatch() {
        assertThrows(ValidationException.class, () -> systemParameterService.patch(
                "currency.code",
                new SystemParameterPatchRequest(null, "VND", true, "invalid runtime flag"),
                "ADMIN",
                "admin-user-1"
        ));
    }

    @Test
    void patchRejectsBackupSloBeyondPolicy() {
        assertThrows(ValidationException.class, () -> systemParameterService.patch(
                "ops.backup.rpo_minutes",
                new SystemParameterPatchRequest(null, "16", null, "invalid rpo"),
                "ADMIN",
                "admin-user-1"
        ));
    }

    @Test
    void patchRejectsNonRuntimeParameterUpdate() {
        assertThrows(ValidationException.class, () -> systemParameterService.patch(
                "ops.backup.rto_minutes",
                new SystemParameterPatchRequest(null, "60", null, "non-runtime update"),
                "ADMIN",
                "admin-user-1"
        ));
    }
}
