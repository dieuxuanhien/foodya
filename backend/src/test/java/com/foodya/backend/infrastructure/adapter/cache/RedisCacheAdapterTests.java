package com.foodya.backend.infrastructure.adapter.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** The fail-open contract lives in this adapter: every Redis error must look like a miss, never an exception. */
class RedisCacheAdapterTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RedisCacheAdapter adapterFailingWith(RuntimeException failure) {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        when(template.opsForHash()).thenThrow(failure);
        when(template.opsForGeo()).thenThrow(failure);
        when(template.opsForSet()).thenThrow(failure);
        when(template.delete(anyString())).thenThrow(failure);
        when(ops.get(anyString())).thenThrow(failure);
        doThrow(failure).when(ops).set(anyString(), anyString(), any(Duration.class));
        return new RedisCacheAdapter(template, new JacksonJsonCodec(objectMapper));
    }

    @Test
    void everyOperationDegradesToMissWhenRedisIsDown() {
        RedisCacheAdapter adapter = adapterFailingWith(new RedisConnectionFailureException("down"));

        assertThat(adapter.get("k", String.class)).isEmpty();
        assertThat(adapter.getHash("h", "f", String.class)).isEmpty();
        assertThat(adapter.getHashAll("h", String.class)).isEmpty();
        assertThat(adapter.getSet("s")).isEmpty();
        assertThat(adapter.geoRadius("g", 106.6, 10.7, 1.0, 10)).isEmpty();

        // These must not throw either.
        adapter.put("k", "v", Duration.ofMinutes(1));
        adapter.evict("k");
        adapter.putHash("h", "f", "v", Duration.ofMinutes(1));
        adapter.evictHash("h", "f");
        adapter.addToSet("s", "m", Duration.ofMinutes(1));
        adapter.evictSet("s");
        adapter.geoAdd("g", 106.6, 10.7, "m");
        adapter.geoRemove("g", "m");
    }

    @Test
    void valuesRoundTripAsJson() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        when(ops.get("k")).thenReturn("{\"lat\":10.5,\"lng\":106.5,\"name\":\"x\"}");
        RedisCacheAdapter adapter = new RedisCacheAdapter(template, new JacksonJsonCodec(objectMapper));

        Point point = adapter.get("k", Point.class).orElseThrow();

        assertThat(point.lat).isEqualTo(10.5);
        assertThat(point.name).isEqualTo("x");
    }

    public static class Point {
        public double lat;
        public double lng;
        public String name;
    }
}
