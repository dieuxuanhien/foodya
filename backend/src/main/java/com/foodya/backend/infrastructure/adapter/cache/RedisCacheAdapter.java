package com.foodya.backend.infrastructure.adapter.cache;

import com.foodya.backend.application.ports.out.CachePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

/**
 * Redis-backed cache. Values are stored as JSON strings so the application layer stays free of
 * Redis/Jackson types. Every operation is fail-open: logistics and chat must keep working with Redis
 * down, so backend errors are logged and treated as a cache miss.
 */
public class RedisCacheAdapter implements CachePort {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheAdapter.class);

    private final StringRedisTemplate redis;
    private final JsonCodec codec;

    public RedisCacheAdapter(StringRedisTemplate redis, JsonCodec codec) {
        this.redis = redis;
        this.codec = codec;
    }

    /** Small seam so the JSON layer is injectable in tests. */
    public interface JsonCodec {
        String toJson(Object value);

        <T> T fromJson(String json, Class<T> type);
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        return guarded(() -> {
            String raw = redis.opsForValue().get(key);
            return raw == null ? Optional.<T>empty() : Optional.ofNullable(codec.fromJson(raw, type));
        }, Optional.empty());
    }

    @Override
    public <T> void put(String key, T value, Duration ttl) {
        if (value == null) return;
        guarded(() -> {
            redis.opsForValue().set(key, codec.toJson(value), ttl);
            return null;
        }, null);
    }

    @Override
    public void evict(String key) {
        guarded(() -> {
            redis.delete(key);
            return null;
        }, null);
    }

    @Override
    public <T> void putHash(String hashKey, String field, T value, Duration ttl) {
        if (value == null) return;
        guarded(() -> {
            redis.opsForHash().put(hashKey, field, codec.toJson(value));
            if (ttl != null) {
                redis.expire(hashKey, ttl);
            }
            return null;
        }, null);
    }

    @Override
    public <T> Optional<T> getHash(String hashKey, String field, Class<T> type) {
        return guarded(() -> {
            Object raw = redis.opsForHash().get(hashKey, field);
            return raw == null ? Optional.<T>empty() : Optional.ofNullable(codec.fromJson(raw.toString(), type));
        }, Optional.empty());
    }

    @Override
    public <T> Map<String, T> getHashAll(String hashKey, Class<T> type) {
        return guarded(() -> {
            Map<Object, Object> raw = redis.opsForHash().entries(hashKey);
            Map<String, T> result = new HashMap<>();
            raw.forEach((k, v) -> {
                T value = codec.fromJson(v.toString(), type);
                if (value != null) {
                    result.put(k.toString(), value);
                }
            });
            return result;
        }, Map.of());
    }

    @Override
    public void evictHash(String hashKey, String field) {
        guarded(() -> {
            redis.opsForHash().delete(hashKey, field);
            return null;
        }, null);
    }

    @Override
    public void addToSet(String setKey, String member, Duration ttl) {
        guarded(() -> {
            redis.opsForSet().add(setKey, member);
            if (ttl != null) {
                redis.expire(setKey, ttl);
            }
            return null;
        }, null);
    }

    @Override
    public Set<String> getSet(String setKey) {
        return guarded(() -> {
            Set<String> members = redis.opsForSet().members(setKey);
            return members == null ? Set.<String>of() : members;
        }, Set.of());
    }

    @Override
    public void evictSet(String setKey) {
        guarded(() -> {
            redis.delete(setKey);
            return null;
        }, null);
    }

    @Override
    public void geoAdd(String geoKey, double lng, double lat, String member) {
        guarded(() -> {
            redis.opsForGeo().add(geoKey, new Point(lng, lat), member);
            return null;
        }, null);
    }

    @Override
    public Map<String, Double> geoRadius(String geoKey, double lng, double lat, double radiusKm, int limit) {
        return guarded(() -> {
            GeoOperations<String, String> geo = redis.opsForGeo();
            GeoResults<RedisGeoCommands.GeoLocation<String>> results = geo.radius(
                    geoKey,
                    new Circle(new Point(lng, lat), new Distance(radiusKm, Metrics.KILOMETERS)),
                    RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                            .includeDistance().sortAscending().limit(limit));
            if (results == null) {
                return Map.<String, Double>of();
            }
            Map<String, Double> out = new LinkedHashMap<>();
            for (GeoResult<RedisGeoCommands.GeoLocation<String>> r : results) {
                out.put(r.getContent().getName(), r.getDistance().getValue());
            }
            return out;
        }, Map.of());
    }

    @Override
    public void geoRemove(String geoKey, String member) {
        guarded(() -> {
            redis.opsForGeo().remove(geoKey, member);
            return null;
        }, null);
    }

    private <T> T guarded(Supplier<T> action, T fallback) {
        try {
            return action.get();
        } catch (RuntimeException ex) {
            log.warn("Redis cache operation failed, treating as miss: {}", ex.getMessage());
            return fallback;
        }
    }
}
