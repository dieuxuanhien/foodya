package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.usecases.HealthReadinessService;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    private final HealthReadinessService healthReadinessService;

    public HealthController(HealthReadinessService healthReadinessService) {
        this.healthReadinessService = healthReadinessService;
    }

    @GetMapping("/live")
    public ResponseEntity<ApiSuccessResponse<Map<String, String>>> liveness(HttpServletRequest request) {
        return ResponseEntity.ok(ApiSuccessResponse.of(Map.of("status", "UP"), RequestTrace.from(request)));
    }

    @GetMapping("/ready")
    public ResponseEntity<ApiSuccessResponse<Map<String, String>>> readiness(HttpServletRequest request) {
        if (healthReadinessService.isReady()) {
            return ResponseEntity.ok(ApiSuccessResponse.of(Map.of("status", "UP"), RequestTrace.from(request)));
        }

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiSuccessResponse.of(Map.of("status", "DOWN"), RequestTrace.from(request)));
    }
}
