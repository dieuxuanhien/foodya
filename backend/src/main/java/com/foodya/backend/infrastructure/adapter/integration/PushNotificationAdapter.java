package com.foodya.backend.infrastructure.adapter.integration;

import com.foodya.backend.application.constants.IntegrationKeyCatalog;
import com.foodya.backend.application.ports.out.PushNotificationPort;
import com.foodya.backend.application.ports.out.DeviceTokenPort;
import com.foodya.backend.domain.entities.DeviceToken;
import com.foodya.backend.infrastructure.config.ApiSecretsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class PushNotificationAdapter implements PushNotificationPort {

    private final DeviceTokenPort deviceTokenPort;
    private final ApiSecretsProvider apiSecretsProvider;
    private FirebaseMessaging firebaseMessaging;
    private boolean initializationAttempted;

    public PushNotificationAdapter(DeviceTokenPort deviceTokenPort,
                                   ApiSecretsProvider apiSecretsProvider) {
        this.deviceTokenPort = deviceTokenPort;
        this.apiSecretsProvider = apiSecretsProvider;
    }

    @Override
    public DeliveryResult sendToUser(UUID receiverUserId, String title, String message, UUID orderId, String eventType) {
        List<DeviceToken> tokens = deviceTokenPort.findEnabledByUserId(receiverUserId);
        if (tokens.isEmpty()) {
            return new DeliveryResult(false, "device-token-not-configured");
        }

        FirebaseMessaging messaging = messaging();
        if (messaging == null) {
            return new DeliveryResult(false, "firebase-admin-not-configured");
        }

        List<String> results = new ArrayList<>();
        int delivered = 0;
        for (DeviceToken token : tokens) {
            try {
                Message.Builder builder = Message.builder()
                        .setToken(token.getToken())
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(message)
                                .build())
                        .putData("eventType", eventType == null ? "" : eventType);
                if (orderId != null) {
                    builder.putData("orderId", orderId.toString());
                }
                String providerId = messaging.send(builder.build());
                delivered++;
                results.add("sent:" + providerId);
            } catch (FirebaseMessagingException ex) {
                results.add("failed:" + ex.getMessagingErrorCode() + ":" + ex.getMessage());
                String errorCode = ex.getMessagingErrorCode() == null ? "" : ex.getMessagingErrorCode().name();
                if ("UNREGISTERED".equals(errorCode) || "INVALID_ARGUMENT".equals(errorCode)) {
                    deviceTokenPort.disableToken(receiverUserId, token.getToken());
                }
            }
        }

        return new DeliveryResult(delivered > 0, String.join(";", results));
    }

    private synchronized FirebaseMessaging messaging() {
        if (firebaseMessaging != null) {
            return firebaseMessaging;
        }
        if (initializationAttempted) {
            return null;
        }

        initializationAttempted = true;
        try (InputStream inputStream = serviceAccountStream()) {
            if (inputStream == null) {
                return null;
            }
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(inputStream))
                    .build();
            FirebaseApp app = FirebaseApp.getApps().stream()
                    .filter(candidate -> "foodya".equals(candidate.getName()))
                    .findFirst()
                    .orElseGet(() -> FirebaseApp.initializeApp(options, "foodya"));
            firebaseMessaging = FirebaseMessaging.getInstance(app);
            return firebaseMessaging;
        } catch (IOException | IllegalStateException ex) {
            return null;
        }
    }

    private InputStream serviceAccountStream() throws IOException {
        return apiSecretsProvider.get(IntegrationKeyCatalog.FIREBASE_SERVICE_ACCOUNT_JSON)
                .<InputStream>map(value -> new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)))
                .orElseGet(() -> apiSecretsProvider.get(IntegrationKeyCatalog.FIREBASE_SERVICE_ACCOUNT_FILE)
                        .map(path -> {
                            try {
                                return new FileInputStream(path);
                            } catch (IOException ex) {
                                return null;
                            }
                        })
                        .orElse(null));
    }
}
