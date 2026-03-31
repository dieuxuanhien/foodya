package com.foodya.backend.application.service;

import com.foodya.backend.domain.model.ParameterValueType;
import com.foodya.backend.domain.persistence.SystemParameter;
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
        List<SystemParameter> parameters = systemParameterService.listAll();
        assertTrue(parameters.stream().anyMatch(p -> p.getKey().equals("shipping.base_delivery_fee")));
        assertTrue(parameters.stream().anyMatch(p -> p.getKey().equals("ops.backup.rto_minutes")));
    }

    @Test
    void patchUpdatesValueAndVersion() {
        SystemParameter before = systemParameterService.listAll().stream()
                .filter(p -> p.getKey().equals("shipping.base_delivery_fee"))
                .findFirst()
                .orElseThrow();
        int beforeVersion = before.getVersion();

        SystemParameter updated = systemParameterService.patch(
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
}
