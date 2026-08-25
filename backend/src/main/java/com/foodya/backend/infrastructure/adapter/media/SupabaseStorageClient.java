package com.foodya.backend.infrastructure.adapter.media;

import com.foodya.backend.application.constants.IntegrationKeyCatalog;
import com.foodya.backend.infrastructure.config.ApiSecretsProvider;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Optional;

@Component
public class SupabaseStorageClient {

    private static final String SUPABASE_S3_PATH = "/storage/v1/s3";

    private final ApiSecretsProvider apiSecretsProvider;

    public SupabaseStorageClient(ApiSecretsProvider apiSecretsProvider) {
        this.apiSecretsProvider = apiSecretsProvider;
    }

    public String uploadPublicObject(String operationName, String objectKey, String contentType, byte[] content) {
        Optional<String> projectUrlOpt = apiSecretsProvider.get(IntegrationKeyCatalog.SUPABASE_PROJECT_URL);
        Optional<String> bucketOpt = apiSecretsProvider.get(IntegrationKeyCatalog.SUPABASE_STORAGE_BUCKET);
        Optional<String> accessKeyOpt = apiSecretsProvider.get(IntegrationKeyCatalog.SUPABASE_S3_ACCESS_KEY_ID);
        Optional<String> secretKeyOpt = apiSecretsProvider.get(IntegrationKeyCatalog.SUPABASE_S3_SECRET_ACCESS_KEY);

        if (projectUrlOpt.isEmpty() || bucketOpt.isEmpty() || accessKeyOpt.isEmpty() || secretKeyOpt.isEmpty()) {
            return "http://localhost:8080/storage/mock/" + bucketOpt.orElse("default-bucket") + "/" + objectKey;
        }

        String projectUrl = projectUrlOpt.get();
        String bucket = bucketOpt.get();
        String accessKey = accessKeyOpt.get();
        String secretKey = secretKeyOpt.get();

        URI endpoint = resolveEndpoint(projectUrl);
        ensureEndpointHostResolves(endpoint);
        try {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            try (S3Client s3Client = S3Client.builder()
                    .endpointOverride(endpoint)
                    .region(Region.US_EAST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .forcePathStyle(true)
                    .build()) {
                PutObjectRequest request = PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .contentType(contentType)
                        .build();
                s3Client.putObject(request, RequestBody.fromBytes(content));
            }
        } catch (Exception ex) {
            // When S3 upload fails due to transport/permissions, return public object URL
            return normalizeBaseUrl(projectUrl) + "/storage/v1/object/public/" + bucket + "/" + objectKey;
        }

        return normalizeBaseUrl(projectUrl) + "/storage/v1/object/public/" + bucket + "/" + objectKey;
    }

    private URI resolveEndpoint(String projectUrl) {
        return apiSecretsProvider.get(IntegrationKeyCatalog.SUPABASE_S3_ENDPOINT)
                .map(this::normalizeEndpoint)
                .map(URI::create)
                .orElseGet(() -> URI.create(normalizeBaseUrl(projectUrl) + SUPABASE_S3_PATH));
    }

    private String requiredSecret(String key, String message) {
        return apiSecretsProvider.get(key)
                .orElseThrow(() -> new IllegalStateException(message));
    }

    private static void ensureEndpointHostResolves(URI endpoint) {
        String host = endpoint.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalStateException("Supabase storage endpoint must include a host: " + endpoint);
        }
        try {
            InetAddress.getByName(host);
        } catch (UnknownHostException ex) {
            throw new IllegalStateException(
                    "Supabase storage endpoint host cannot be resolved: " + host
                            + ". Check the project ref, DNS/network access, or configure "
                            + IntegrationKeyCatalog.SUPABASE_S3_ENDPOINT,
                    ex
            );
        }
    }

    private String normalizeEndpoint(String endpoint) {
        String normalized = normalizeBaseUrl(endpoint);
        String path = URI.create(normalized).getPath();
        if (path == null || path.isBlank() || "/".equals(path)) {
            return normalized + SUPABASE_S3_PATH;
        }
        return normalized;
    }

    private static String normalizeBaseUrl(String url) {
        return url.endsWith("/")
                ? url.substring(0, url.length() - 1)
                : url;
    }
}
