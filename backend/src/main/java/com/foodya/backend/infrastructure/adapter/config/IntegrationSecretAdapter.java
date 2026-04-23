package com.foodya.backend.infrastructure.adapter.config;

import com.foodya.backend.application.ports.out.IntegrationSecretPort;
import com.foodya.backend.infrastructure.config.ApiSecretsProvider;
import org.springframework.stereotype.Component;

@Component
public class IntegrationSecretAdapter implements IntegrationSecretPort {

    private final ApiSecretsProvider apiSecretsProvider;

    public IntegrationSecretAdapter(ApiSecretsProvider apiSecretsProvider) {
        this.apiSecretsProvider = apiSecretsProvider;
    }

    @Override
    public boolean isConfigured(String key) {
        return apiSecretsProvider.isConfigured(key);
    }
}
