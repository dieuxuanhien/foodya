package com.foodya.backend.infrastructure.integration;

import com.foodya.backend.application.ports.out.FirebaseConfigPort;
import com.foodya.backend.infrastructure.config.ApiSecretsProvider;
import com.foodya.backend.infrastructure.config.IntegrationKey;
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
                "apiKey", apiSecretsProvider.get(IntegrationKey.FIREBASE_API_KEY).orElse(""),
                "authDomain", apiSecretsProvider.get(IntegrationKey.FIREBASE_AUTH_DOMAIN).orElse(""),
                "projectId", apiSecretsProvider.get(IntegrationKey.FIREBASE_PROJECT_ID).orElse(""),
                "storageBucket", apiSecretsProvider.get(IntegrationKey.FIREBASE_STORAGE_BUCKET).orElse(""),
                "messagingSenderId", apiSecretsProvider.get(IntegrationKey.FIREBASE_MESSAGING_SENDER_ID).orElse(""),
                "appId", apiSecretsProvider.get(IntegrationKey.FIREBASE_APP_ID).orElse(""),
                "measurementId", apiSecretsProvider.get(IntegrationKey.FIREBASE_MEASUREMENT_ID).orElse("")
        );
    }
}
