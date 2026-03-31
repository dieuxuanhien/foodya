package com.foodya.backend.application.port.out;

public interface IntegrationSecretPort {

    boolean isConfigured(String key);
}
