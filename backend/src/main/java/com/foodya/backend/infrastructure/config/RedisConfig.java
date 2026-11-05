package com.foodya.backend.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;

/**
 * Redis support for the cache layer. {@code spring-boot-starter-data-redis} auto-configures a RedisConnectionFactory,
 * but the short command timeout and the explicit string template are set here so a slow/unreachable Redis fails
 * fast instead of stalling request threads.
 *
 * <p>When {@code spring.cache.enabled=false} no template is registered; the cache beans fall back to a no-op
 * adapter so every CachePort consumer keeps working without Redis.
 */
@Configuration
public class RedisConfig {

    @Bean
    @ConditionalOnProperty(prefix = "spring.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
    public LettuceConnectionFactory redisConnectionFactory(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${spring.data.redis.timeout:2s}") Duration timeout) {
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(host, port);
        LettuceClientConfiguration client = LettuceClientConfiguration.builder()
                .commandTimeout(timeout)
                .shutdownTimeout(Duration.ofMillis(200))
                .build();
        return new LettuceConnectionFactory(standalone, client);
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
