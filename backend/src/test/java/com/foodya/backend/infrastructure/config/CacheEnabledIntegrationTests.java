package com.foodya.backend.infrastructure.config;

import com.foodya.backend.application.ports.out.CachePort;
import com.foodya.backend.infrastructure.adapter.cache.RedisCacheAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for a real bug: {@code @ConditionalOnBean(StringRedisTemplate.class)} in a
 * {@code @Configuration} that Spring processes before {@link RedisConfig} silently resolved to the no-op
 * cache even with Redis enabled and reachable — the condition is evaluated at configuration-processing
 * time, not bean-creation time. Fixed by gating both beans on the same property and importing
 * {@link RedisConfig} explicitly so its template bean is always registered first.
 */
@SpringBootTest(properties = "spring.cache.enabled=true")
@ActiveProfiles("test")
class CacheEnabledIntegrationTests {

    @Autowired
    private CachePort cachePort;

    @Test
    void redisAdapterIsSelectedWhenCacheIsEnabled() {
        assertThat(cachePort).isInstanceOf(RedisCacheAdapter.class);
    }
}
