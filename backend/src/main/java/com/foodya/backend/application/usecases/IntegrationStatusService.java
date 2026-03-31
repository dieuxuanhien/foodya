package com.foodya.backend.application.usecases;

import com.foodya.backend.application.constant.IntegrationKeyCatalog;
import com.foodya.backend.domain.value_objects.IntegrationStatus;
import com.foodya.backend.application.ports.out.IntegrationSecretPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IntegrationStatusService {

    private final IntegrationSecretPort integrationSecretPort;

    public IntegrationStatusService(IntegrationSecretPort integrationSecretPort) {
        this.integrationSecretPort = integrationSecretPort;
    }

    public List<IntegrationStatus> getStatuses() {
        return List.of(
            statusOf(IntegrationKeyCatalog.GOOGLE_AI_STUDIO_API_KEY),
            statusOf(IntegrationKeyCatalog.SUPABASE_PROJECT_URL),
            statusOf(IntegrationKeyCatalog.SUPABASE_S3_ACCESS_KEY_ID),
            statusOf(IntegrationKeyCatalog.SUPABASE_S3_SECRET_ACCESS_KEY),
            statusOf(IntegrationKeyCatalog.GOONG_API_KEY),
            statusOf(IntegrationKeyCatalog.FIREBASE_API_KEY),
            statusOf(IntegrationKeyCatalog.FIREBASE_AUTH_DOMAIN),
            statusOf(IntegrationKeyCatalog.FIREBASE_PROJECT_ID),
            statusOf(IntegrationKeyCatalog.FIREBASE_STORAGE_BUCKET),
            statusOf(IntegrationKeyCatalog.FIREBASE_MESSAGING_SENDER_ID),
            statusOf(IntegrationKeyCatalog.FIREBASE_APP_ID),
            statusOf(IntegrationKeyCatalog.FIREBASE_MEASUREMENT_ID),
            statusOf(IntegrationKeyCatalog.OPENWEATHER_API_KEY)
        );
    }

    private IntegrationStatus statusOf(String key) {
        boolean configured = integrationSecretPort.isConfigured(key);
        return new IntegrationStatus(key, configured, configured ? "env-or-json" : "missing");
    }
}
