package com.foodya.backend.application.usecases;

import com.foodya.backend.application.ports.out.HealthCheckPort;
import org.springframework.stereotype.Service;

@Service
public class HealthReadinessService {

    private final HealthCheckPort healthCheckPort;

    public HealthReadinessService(HealthCheckPort healthCheckPort) {
        this.healthCheckPort = healthCheckPort;
    }

    public boolean isReady() {
        return healthCheckPort.isDatabaseReady();
    }
}