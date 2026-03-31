package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.usecases.IntegrationProbeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/system/integrations")
@Tag(name = "System Integrations", description = "Integration probe endpoints")
public class IntegrationProbeController {

    private final IntegrationProbeService integrationProbeService;

    public IntegrationProbeController(IntegrationProbeService integrationProbeService) {
        this.integrationProbeService = integrationProbeService;
    }

    @GetMapping("/firebase-config")
    @Operation(summary = "Firebase config probe", description = "Returns non-sensitive Firebase config fields used by clients")
    public ResponseEntity<Map<String, String>> firebaseConfig() {
        return ResponseEntity.ok(integrationProbeService.firebaseWebConfig());
    }

    @GetMapping("/supabase-config")
    @Operation(summary = "Supabase config probe", description = "Returns project URL and whether storage credentials are configured")
    public ResponseEntity<Map<String, String>> supabaseConfig() {
        return ResponseEntity.ok(integrationProbeService.supabaseConfigSummary());
    }
}
