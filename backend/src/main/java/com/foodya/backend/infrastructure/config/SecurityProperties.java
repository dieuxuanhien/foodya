package com.foodya.backend.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "foodya.security")
public class SecurityProperties {

    private String jwtSecret;
    private long accessTokenMinutes = 15;
    private long refreshTokenDays = 30;
    private long resetTokenMinutes = 10;
    private long otpExpiryMinutes = 10;

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public long getAccessTokenMinutes() {
        return accessTokenMinutes;
    }

    public void setAccessTokenMinutes(long accessTokenMinutes) {
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public long getRefreshTokenDays() {
        return refreshTokenDays;
    }

    public void setRefreshTokenDays(long refreshTokenDays) {
        this.refreshTokenDays = refreshTokenDays;
    }

    public long getResetTokenMinutes() {
        return resetTokenMinutes;
    }

    public void setResetTokenMinutes(long resetTokenMinutes) {
        this.resetTokenMinutes = resetTokenMinutes;
    }

    public long getOtpExpiryMinutes() {
        return otpExpiryMinutes;
    }

    public void setOtpExpiryMinutes(long otpExpiryMinutes) {
        this.otpExpiryMinutes = otpExpiryMinutes;
    }
}
