package com.foodya.backend.infrastructure.config;

import com.foodya.backend.application.ports.out.CachePort;
import com.foodya.backend.infrastructure.adapter.cache.NoOpCacheAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.cache.enabled=false")
@ActiveProfiles("test")
class CacheDisabledIntegrationTests {

    @Autowired
    private CachePort cachePort;

    @Test
    void noOpAdapterIsSelectedWhenCacheIsDisabled() {
        assertThat(cachePort).isInstanceOf(NoOpCacheAdapter.class);
    }
}
