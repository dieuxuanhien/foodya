package com.foodya.backend.infrastructure.adapter.cache;

import com.foodya.backend.application.ports.out.CachePort;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Cache used when {@code spring.cache.enabled=false}. Every read misses and every write is dropped, so the
 * application behaves exactly as it did before caching was introduced.
 */
public class NoOpCacheAdapter implements CachePort {

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        return Optional.empty();
    }

    @Override
    public <T> void put(String key, T value, Duration ttl) {
    }

    @Override
    public void evict(String key) {
    }

    @Override
    public <T> void putHash(String hashKey, String field, T value, Duration ttl) {
    }

    @Override
    public <T> Optional<T> getHash(String hashKey, String field, Class<T> type) {
        return Optional.empty();
    }

    @Override
    public <T> Map<String, T> getHashAll(String hashKey, Class<T> type) {
        return Map.of();
    }

    @Override
    public void evictHash(String hashKey, String field) {
    }

    @Override
    public void addToSet(String setKey, String member, Duration ttl) {
    }

    @Override
    public Set<String> getSet(String setKey) {
        return Set.of();
    }

    @Override
    public void evictSet(String setKey) {
    }

    @Override
    public void geoAdd(String geoKey, double lng, double lat, String member) {
    }

    @Override
    public Map<String, Double> geoRadius(String geoKey, double lng, double lat, double radiusKm, int limit) {
        return Map.of();
    }

    @Override
    public void geoRemove(String geoKey, String member) {
    }
}
