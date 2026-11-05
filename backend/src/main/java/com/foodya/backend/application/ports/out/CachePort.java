package com.foodya.backend.application.ports.out;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Generic key/value + hash cache abstraction. Implementations must be fail-open: any backend error
 * (Redis down, serialization failure) results in a miss / no-op, never an exception reaching the caller.
 */
public interface CachePort {

    <T> Optional<T> get(String key, Class<T> type);

    <T> void put(String key, T value, Duration ttl);

    void evict(String key);

    <T> void putHash(String hashKey, String field, T value, Duration ttl);

    <T> Optional<T> getHash(String hashKey, String field, Class<T> type);

    <T> Map<String, T> getHashAll(String hashKey, Class<T> type);

    /** Removes one field from a hash; no-op when absent. */
    void evictHash(String hashKey, String field);

    /** Adds a member to a set with a TTL on the whole key. Used to index drivers by H3 cell. */
    void addToSet(String setKey, String member, Duration ttl);

    Set<String> getSet(String setKey);

    void evictSet(String setKey);

    /**
     * Geospatial index update (lng, lat in WGS84 degrees).
     */
    void geoAdd(String geoKey, double lng, double lat, String member);

    /**
     * Members within {@code radiusKm} of the centre, nearest first.
     */
    Map<String, Double> geoRadius(String geoKey, double lng, double lat, double radiusKm, int limit);

    void geoRemove(String geoKey, String member);
}
