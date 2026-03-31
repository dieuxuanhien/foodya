package com.foodya.backend.application.port.out;

public interface HealthCheckPort {

    boolean isDatabaseReady();
}