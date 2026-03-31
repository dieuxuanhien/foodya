package com.foodya.backend.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "foodya.rate-limit")
public class RateLimitProperties {

    private int authRequestsPerMinute = 60;
    private int aiRequestsPerMinute = 30;

    public int getAuthRequestsPerMinute() {
        return authRequestsPerMinute;
    }

    public void setAuthRequestsPerMinute(int authRequestsPerMinute) {
        this.authRequestsPerMinute = authRequestsPerMinute;
    }

    public int getAiRequestsPerMinute() {
        return aiRequestsPerMinute;
    }

    public void setAiRequestsPerMinute(int aiRequestsPerMinute) {
        this.aiRequestsPerMinute = aiRequestsPerMinute;
    }
}
