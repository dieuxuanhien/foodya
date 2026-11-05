package com.foodya.backend.application.usecases;

import com.foodya.backend.application.dto.WeatherData;
import com.foodya.backend.application.ports.out.CachePort;
import com.foodya.backend.application.ports.out.GeoPort;
import com.foodya.backend.application.ports.out.WeatherContextPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;

/**
 * Weather lookup behind a shared cache keyed by H3 res8 cell.
 *
 * <p>Why res8 and not res9: weather is uniform over roughly a square kilometre, so caching per res9
 * (~0.1 km²) would multiply upstream calls by ~7 for no accuracy gain.
 *
 * <p>Why the cache goes through {@link CachePort} (Redis) instead of an in-JVM map: multiple backend
 * instances must share the same weather window, and the TTL is then enforced by Redis rather than by a
 * per-process expiry check.
 *
 * <p>"Not available" is cached too (short TTL) so a provider outage does not cause one upstream call
 * per chat message. That state is distinguished from "nothing cached" by a marker entry, because an
 * empty {@code Optional} cannot be stored as a value.
 */
public class WeatherCacheService {

    private static final Logger log = LoggerFactory.getLogger(WeatherCacheService.class);

    private static final int H3_WEATHER_RESOLUTION = 8;
    private static final Duration WEATHER_TTL = Duration.ofMinutes(15);
    private static final Duration UNAVAILABLE_TTL = Duration.ofMinutes(2);
    private static final String KEY_PREFIX = "weather:h3:";
    private static final String UNAVAILABLE_KEY_PREFIX = "weather:h3:unavailable:";

    private final WeatherContextPort weatherContextPort;
    private final CachePort cachePort;
    private final GeoPort geoPort;

    public WeatherCacheService(WeatherContextPort weatherContextPort, CachePort cachePort, GeoPort geoPort) {
        this.weatherContextPort = weatherContextPort;
        this.cachePort = cachePort;
        this.geoPort = geoPort;
    }

    public record WeatherResult(WeatherData data, String h3Res8Cell) {
        public boolean available() {
            return data != null;
        }
    }

    public WeatherResult getWeatherForLocation(double lat, double lng) {
        String cell = geoPort.geoToH3Index(lat, lng, H3_WEATHER_RESOLUTION);
        String key = KEY_PREFIX + cell;
        String unavailableKey = UNAVAILABLE_KEY_PREFIX + cell;

        Optional<CachedWeather> cached = cachePort.get(key, CachedWeather.class);
        if (cached.isPresent()) {
            return new WeatherResult(cached.get().data(), cell);
        }
        // Cached negative result: skip the upstream call but do not report stale data.
        if (cachePort.get(unavailableKey, Boolean.class).orElse(false)) {
            return new WeatherResult(null, cell);
        }

        try {
            WeatherData data = weatherContextPort.getCurrentWeather(lat, lng).orElse(null);
            if (data == null) {
                cachePort.put(unavailableKey, Boolean.TRUE, UNAVAILABLE_TTL);
                return new WeatherResult(null, cell);
            }
            cachePort.put(key, new CachedWeather(data), WEATHER_TTL);
            return new WeatherResult(data, cell);
        } catch (RuntimeException ex) {
            log.warn("Weather lookup failed for h3 cell {}: {}", cell, ex.getMessage());
            cachePort.put(unavailableKey, Boolean.TRUE, UNAVAILABLE_TTL);
            return new WeatherResult(null, cell);
        }
    }

    public void evict(String h3Res8Cell) {
        cachePort.evict(KEY_PREFIX + h3Res8Cell);
        cachePort.evict(UNAVAILABLE_KEY_PREFIX + h3Res8Cell);
    }

    /** Stored wrapper — a bare record is needed because {@code Optional.empty()} cannot be cached. */
    public record CachedWeather(WeatherData data) {
        public CachedWeather() {
            this(null);
        }
    }
}
