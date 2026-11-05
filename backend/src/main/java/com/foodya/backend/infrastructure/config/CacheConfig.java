package com.foodya.backend.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodya.backend.application.ports.out.CachePort;
import com.foodya.backend.infrastructure.adapter.cache.JacksonJsonCodec;
import com.foodya.backend.infrastructure.adapter.cache.NoOpCacheAdapter;
import com.foodya.backend.infrastructure.adapter.cache.RedisCacheAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Chooses the CachePort implementation.
 *
 * <p>Both beans are gated on the same property rather than using {@code @ConditionalOnBean(StringRedisTemplate)}
 * here: that condition is evaluated in configuration-processing order, and this class can be processed
 * before {@link RedisConfig} contributes the template, which silently selected the no-op adapter even when
 * Redis was available.
 */
@Configuration
@Import(RedisConfig.class)
public class CacheConfig {

    @Bean
    @ConditionalOnProperty(prefix = "spring.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
    public CachePort redisCachePort(
            org.springframework.data.redis.core.StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        return new RedisCacheAdapter(redisTemplate, new JacksonJsonCodec(objectMapper));
    }

    @Bean
    @ConditionalOnMissingBean(CachePort.class)
    public CachePort noOpCachePort() {
        return new NoOpCacheAdapter();
    }
}
