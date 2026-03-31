package com.foodya.backend.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
	SecretSourcesProperties.class,
	SecurityProperties.class,
	RateLimitProperties.class
})
public class AppConfig {
}
