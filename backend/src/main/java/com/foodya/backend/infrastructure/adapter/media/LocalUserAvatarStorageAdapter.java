package com.foodya.backend.infrastructure.adapter.media;

import com.foodya.backend.application.ports.out.UserAvatarStoragePort;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Component
public class LocalUserAvatarStorageAdapter implements UserAvatarStoragePort {

    private final SupabaseStorageClient storageClient;

    public LocalUserAvatarStorageAdapter(SupabaseStorageClient storageClient) {
        this.storageClient = storageClient;
    }

    @Override
    public String store(UUID userId, String originalFileName, String contentType, byte[] content) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(content, "content");

        String extension = resolveExtension(originalFileName);
        String objectKey = "user-avatars/" + userId + "/" + UUID.randomUUID() + extension;

        return storageClient.uploadPublicObject(
                "user avatar",
                objectKey,
                resolveContentType(contentType, extension),
                content
        );
    }

    private static String resolveExtension(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return ".bin";
        }
        String lower = originalFileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return ".jpg";
        if (lower.endsWith(".png")) return ".png";
        if (lower.endsWith(".webp")) return ".webp";
        if (lower.endsWith(".gif")) return ".gif";
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

}
