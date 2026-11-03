package com.foodya.backend.application.usecases;

import com.foodya.backend.application.ports.out.CachePort;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Minimal in-memory CachePort for unit tests (TTL expiry is simulated with an explicit clock). */
public final class InMemoryCache implements CachePort {

    private final Map<String, Object> values = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sets = new ConcurrentHashMap<>();
    private final Map<String, Map<String, double[]>> geo = new ConcurrentHashMap<>();
    private volatile boolean failAll;

    public void failAll(boolean fail) {
        this.failAll = fail;
    }

    private void maybeFail() {
        if (failAll) throw new IllegalStateException("redis unavailable");
    }

    @Override public <T> Optional<T> get(String key, Class<T> type) {
        maybeFail();
        return Optional.ofNullable(type.cast(values.get(key)));
    }

    @Override public <T> void put(String key, T value, Duration ttl) {
        maybeFail();
        if (value != null) values.put(key, value);
    }

    @Override public void evict(String key) {
        maybeFail();
        values.remove(key);
    }

    @Override public <T> void putHash(String hashKey, String field, T value, Duration ttl) {
        maybeFail();
        values.put(hashKey + "#" + field, value);
    }

    @Override public <T> Optional<T> getHash(String hashKey, String field, Class<T> type) {
        maybeFail();
        return Optional.ofNullable(type.cast(values.get(hashKey + "#" + field)));
    }

    @Override public <T> Map<String, T> getHashAll(String hashKey, Class<T> type) {
        maybeFail();
        Map<String, T> out = new HashMap<>();
        values.forEach((k, v) -> {
            if (k.startsWith(hashKey + "#")) out.put(k.substring(hashKey.length() + 1), type.cast(v));
        });
        return out;
    }

    @Override public void evictHash(String hashKey, String field) {
        maybeFail();
        values.remove(hashKey + "#" + field);
    }

    @Override public void addToSet(String setKey, String member, Duration ttl) {
        maybeFail();
        sets.computeIfAbsent(setKey, k -> ConcurrentHashMap.newKeySet()).add(member);
    }

    @Override public Set<String> getSet(String setKey) {
        maybeFail();
        return new HashSet<>(sets.getOrDefault(setKey, Set.of()));
    }

    @Override public void evictSet(String setKey) {
        maybeFail();
        sets.remove(setKey);
    }

    @Override public void geoAdd(String geoKey, double lng, double lat, String member) {
        maybeFail();
        geo.computeIfAbsent(geoKey, k -> new ConcurrentHashMap<>()).put(member, new double[]{lng, lat});
    }

    @Override public Map<String, Double> geoRadius(String geoKey, double lng, double lat, double radiusKm, int limit) {
        maybeFail();
        Map<String, Double> out = new LinkedHashMap<>();
        geo.getOrDefault(geoKey, Map.of()).forEach((m, pos) -> {
            double d = haversineKm(lat, lng, pos[1], pos[0]);
            if (d <= radiusKm) out.put(m, d);
        });
        return out.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(limit)
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    @Override public void geoRemove(String geoKey, String member) {
        maybeFail();
        Optional.ofNullable(geo.get(geoKey)).ifPresent(g -> g.remove(member));
    }

    private static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1), dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
