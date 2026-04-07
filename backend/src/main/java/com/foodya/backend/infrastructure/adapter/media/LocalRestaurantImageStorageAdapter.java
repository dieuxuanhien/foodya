package com.foodya.backend.infrastructure.adapter.media;

import com.foodya.backend.application.constants.IntegrationKeyCatalog;
import com.foodya.backend.application.ports.out.RestaurantImageStoragePort;
import com.foodya.backend.infrastructure.config.ApiSecretsProvider;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Component
public class LocalRestaurantImageStorageAdapter implements RestaurantImageStoragePort {

    private final ApiSecretsProvider apiSecretsProvider;

    public LocalRestaurantImageStorageAdapter(ApiSecretsProvider apiSecretsProvider) {
        this.apiSecretsProvider = apiSecretsProvider;
    }

    @Override
    public String store(UUID restaurantId, String originalFileName, String contentType, byte[] content) {
        Objects.requireNonNull(restaurantId, "restaurantId");
        Objects.requireNonNull(content, "content");

        String projectUrl = apiSecretsProvider.get(IntegrationKeyCatalog.SUPABASE_PROJECT_URL)
                .orElseThrow(() -> new IllegalStateException("Missing Supabase project URL"));
        String bucket = apiSecretsProvider.get(IntegrationKeyCatalog.SUPABASE_STORAGE_BUCKET)
                .orElseThrow(() -> new IllegalStateException("Missing Supabase storage bucket"));
        String accessKey = apiSecretsProvider.get(IntegrationKeyCatalog.SUPABASE_S3_ACCESS_KEY_ID)
                .orElseThrow(() -> new IllegalStateException("Missing Supabase S3 access key"));
        String secretKey = apiSecretsProvider.get(IntegrationKeyCatalog.SUPABASE_S3_SECRET_ACCESS_KEY)
                .orElseThrow(() -> new IllegalStateException("Missing Supabase S3 secret key"));

        String extension = resolveExtension(originalFileName);
        String objectKey = "restaurants/" + restaurantId + "/" + UUID.randomUUID() + extension;

        URI endpoint = URI.create(normalizeProjectUrl(projectUrl) + "/storage/v1/s3");
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
                    .contentType(resolveContentType(contentType, extension))
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(content));
        } catch (Exception ex) {
            throw new IllegalStateException("failed to upload restaurant image to Supabase", ex);
        }

        return normalizeProjectUrl(projectUrl) + "/storage/v1/object/public/" + bucket + "/" + objectKey;
    }

    private static String resolveExtension(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return ".bin";
        }

        String lower = originalFileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return ".jpg";
        }
        if (lower.endsWith(".png")) {
            return ".png";
        }
        if (lower.endsWith(".webp")) {
            return ".webp";
        }
        if (lower.endsWith(".gif")) {
            return ".gif";
        }
        return ".bin";
    }

    private static String resolveContentType(String contentType, String extension) {
        if (contentType != null && !contentType.isBlank() && contentType.startsWith("image/")) {
            return contentType;
        }
        return switch (extension) {
            case ".jpg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".webp" -> "image/webp";
            case ".gif" -> "image/gif";
            default -> "application/octet-stream";
        };
    }

    private static String normalizeProjectUrl(String projectUrl) {
        if (projectUrl.endsWith("/")) {
            return projectUrl.substring(0, projectUrl.length() - 1);
        }
        return projectUrl;
    }
}
