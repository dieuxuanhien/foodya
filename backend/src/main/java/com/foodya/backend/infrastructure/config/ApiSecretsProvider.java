package com.foodya.backend.infrastructure.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
public class ApiSecretsProvider {

    private static final Logger log = LoggerFactory.getLogger(ApiSecretsProvider.class);

    private final SecretSourcesProperties secretSourcesProperties;
    private final ObjectMapper objectMapper;

    private Dotenv dotenv;
    private Map<String, String> jsonSecrets = Collections.emptyMap();

    public ApiSecretsProvider(SecretSourcesProperties secretSourcesProperties, ObjectMapper objectMapper) {
        this.secretSourcesProperties = secretSourcesProperties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void init() {
        loadDotenv();
        loadJsonSecrets();
    }

    public Optional<String> get(String key) {
        String envValue = getFromEnv(key);
        if (hasText(envValue)) {
            return Optional.of(envValue.trim());
        }

        String jsonValue = jsonSecrets.get(key);
        if (hasText(jsonValue)) {
            return Optional.of(jsonValue.trim());
        }

        return Optional.empty();
    }

    public boolean isConfigured(String key) {
        return get(key).isPresent();
    }

    private void loadDotenv() {
        try {
            dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .ignoreIfMalformed()
                    .load();
        } catch (Exception ex) {
            log.warn("Could not load .env file: {}", ex.getMessage());
        }
    }

    private void loadJsonSecrets() {
        Path path = Path.of(secretSourcesProperties.getJsonFile());
        if (!Files.exists(path)) {
            return;
        }

        try {
            Map<String, String> fileSecrets = objectMapper.readValue(path.toFile(), new TypeReference<>() {
            });
            jsonSecrets = new HashMap<>(fileSecrets);
        } catch (IOException ex) {
            log.warn("Could not read secrets file {}: {}", path, ex.getMessage());
        }
    }

    private String getFromEnv(String key) {
        String envName = toEnvKey(key);
        String direct = System.getenv(envName);
        if (hasText(direct)) {
            return direct;
        }

        if (dotenv == null) {
            return null;
        }
        return dotenv.get(envName);
    }

    private static String toEnvKey(String key) {
        return key.toUpperCase(Locale.ROOT)
                .replace('.', '_')
                .replace('-', '_');
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
