package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.usecases.IntegrationStatusService;
import com.foodya.backend.domain.value_objects.IntegrationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system/integrations")
@Tag(name = "System Integrations", description = "Integration configuration readiness endpoints")
public class IntegrationStatusController {

    private final IntegrationStatusService integrationStatusService;

    public IntegrationStatusController(IntegrationStatusService integrationStatusService) {
        this.integrationStatusService = integrationStatusService;
    }

    @GetMapping("/status")
    @Operation(summary = "Get integration key readiness", description = "Returns key presence status without exposing secret values")
    public ResponseEntity<List<IntegrationStatus>> status() {
        return ResponseEntity.ok(integrationStatusService.getStatuses());
    }
}
