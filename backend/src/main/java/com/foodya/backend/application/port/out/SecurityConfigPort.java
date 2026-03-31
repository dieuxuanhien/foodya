package com.foodya.backend.application.port.out;

/**
 * Contract for security configuration access.
 *
 * <p>
 * Allows application layer to read security configuration without directly importing
 * org.springframework.security or infrastructure config.
 * </p>
 *
 * <p>
 * Phase 2.5 enhancement: Currently security configs are read directly in filters.
 * This port will abstract them for true inversion.
 * </p>
 *
 * <p>
 * Implemented by: infrastructure/adapter/config/SecurityConfigAdapter
 * </p>
 */
public interface SecurityConfigPort {

    /**
     * Get JWT secret key for token signing/validation.
     *
     * @return secret key (minimum 256 bits for HS256)
     */
    String getJwtSecretKey();

    /**
     * Get JWT access token expiration in minutes.
     *
     * @return expiration minutes (e.g., 15)
     */
    long getAccessTokenExpirationMinutes();

    /**
     * Get JWT refresh token expiration in days.
     *
     * @return expiration days (e.g., 30)
     */
    long getRefreshTokenExpirationDays();

    /**
     * Get rate limit attempts per minute for authentication endpoints.
     *
     * @return attempts per minute (e.g. 5)
     */
    int getAuthRateLimitPerMinute();

    /**
     * Check if security feature is enabled.
     *
     * @param featureName feature flag name
     * @return true if enabled
     */
    boolean isSecurityFeatureEnabled(String featureName);
}
