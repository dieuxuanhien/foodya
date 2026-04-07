package com.foodya.backend.infrastructure.adapter.integration;

import com.foodya.backend.application.ports.out.FirebaseConfigPort;
import com.foodya.backend.application.constants.IntegrationKeyCatalog;
import com.foodya.backend.infrastructure.config.ApiSecretsProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FirebasePushAdapter implements FirebaseConfigPort {

    private final ApiSecretsProvider apiSecretsProvider;

    public FirebasePushAdapter(ApiSecretsProvider apiSecretsProvider) {
        this.apiSecretsProvider = apiSecretsProvider;
    }

    @Override
    public Map<String, String> getFirebaseWebConfig() {
        return Map.of(
            "apiKey", apiSecretsProvider.get(IntegrationKeyCatalog.FIREBASE_API_KEY).orElse(""),
            "authDomain", apiSecretsProvider.get(IntegrationKeyCatalog.FIREBASE_AUTH_DOMAIN).orElse(""),
            "projectId", apiSecretsProvider.get(IntegrationKeyCatalog.FIREBASE_PROJECT_ID).orElse(""),
            "storageBucket", apiSecretsProvider.get(IntegrationKeyCatalog.FIREBASE_STORAGE_BUCKET).orElse(""),
            "messagingSenderId", apiSecretsProvider.get(IntegrationKeyCatalog.FIREBASE_MESSAGING_SENDER_ID).orElse(""),
            "appId", apiSecretsProvider.get(IntegrationKeyCatalog.FIREBASE_APP_ID).orElse(""),
            "measurementId", apiSecretsProvider.get(IntegrationKeyCatalog.FIREBASE_MEASUREMENT_ID).orElse("")
        );
    }
}
