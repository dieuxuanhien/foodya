package com.foodya.backend.application.ports.out;

public interface IntegrationSecretPort {

    boolean isConfigured(String key);
}
