package com.foodya.backend.infrastructure.adapter.config;

import com.foodya.backend.application.ports.out.SecurityPolicyPort;
import com.foodya.backend.infrastructure.config.SecurityProperties;
import org.springframework.stereotype.Component;

@Component
public class SecurityPolicyAdapter implements SecurityPolicyPort {

    private final SecurityProperties securityProperties;

    public SecurityPolicyAdapter(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public String jwtSecret() {
        String secret = securityProperties.getJwtSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("foodya.security.jwt-secret must be configured");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException("foodya.security.jwt-secret must be at least 32 characters");
        }
        return secret;
    }

    @Override
    public long accessTokenMinutes() {
        return securityProperties.getAccessTokenMinutes();
    }

    @Override
    public long refreshTokenDays() {
        return securityProperties.getRefreshTokenDays();
    }

    @Override
    public long resetTokenMinutes() {
        return securityProperties.getResetTokenMinutes();
    }

    @Override
    public long otpExpiryMinutes() {
        return securityProperties.getOtpExpiryMinutes();
    }
}
