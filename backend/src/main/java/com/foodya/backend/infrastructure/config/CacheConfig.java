package com.foodya.backend.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodya.backend.application.ports.out.CachePort;
import com.foodya.backend.infrastructure.adapter.cache.JacksonJsonCodec;
import com.foodya.backend.infrastructure.adapter.cache.NoOpCacheAdapter;
import com.foodya.backend.infrastructure.adapter.cache.RedisCacheAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class CacheConfig {

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    public CachePort redisCachePort(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        return new RedisCacheAdapter(redisTemplate, new JacksonJsonCodec(objectMapper));
    }

    /** Used when Redis is disabled (`spring.cache.enabled=false`). */
    @Bean
    @ConditionalOnMissingBean(CachePort.class)
    public CachePort noOpCachePort() {
        return new NoOpCacheAdapter();
    }
}
