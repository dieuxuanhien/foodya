package com.foodya.backend.application.ports.out;

public interface HealthCheckPort {

    boolean isDatabaseReady();
}