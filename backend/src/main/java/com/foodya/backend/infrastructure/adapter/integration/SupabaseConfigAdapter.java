package com.foodya.backend.infrastructure.adapter.integration;

import com.foodya.backend.application.ports.out.SupabaseConfigPort;
import com.foodya.backend.infrastructure.config.ApiSecretsProvider;
import com.foodya.backend.infrastructure.config.IntegrationKey;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SupabaseConfigAdapter implements SupabaseConfigPort {

    private final ApiSecretsProvider apiSecretsProvider;

    public SupabaseConfigAdapter(ApiSecretsProvider apiSecretsProvider) {
        this.apiSecretsProvider = apiSecretsProvider;
    }

    @Override
    public Map<String, String> getSupabaseConfigSummary() {
        return Map.of(
                "projectUrl", apiSecretsProvider.get(IntegrationKey.SUPABASE_PROJECT_URL).orElse(""),
                "s3AccessKeyIdConfigured", String.valueOf(apiSecretsProvider.isConfigured(IntegrationKey.SUPABASE_S3_ACCESS_KEY_ID)),
                "s3SecretConfigured", String.valueOf(apiSecretsProvider.isConfigured(IntegrationKey.SUPABASE_S3_SECRET_ACCESS_KEY))
        );
    }
}
