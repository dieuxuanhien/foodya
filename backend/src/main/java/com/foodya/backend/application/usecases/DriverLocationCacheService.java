package com.foodya.backend.application.usecases;

import com.foodya.backend.application.ports.out.CachePort;
import com.foodya.backend.application.ports.out.DriverLocationCachePort;
import com.foodya.backend.domain.value_objects.DriverCachedLocation;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Redis-backed driver position cache (Phase 9).
 *
 * <p>Driver GPS updates arrive every 3–5 s. Writing each one to PostgreSQL is wasteful, so the hot path
 * is Redis and the DB session row is the durable record synced on meaningful state changes.
 *
 * <p>Two indexes are maintained deliberately:
 * <ul>
 *   <li><b>GEO</b> ({@code driver:locations:geo}) for radius queries — {@code GEOSEARCH} is O(N log M).</li>
 *   <li><b>H3 set per cell</b> ({@code driver:locations:h3:<cell>}) because matching walks H3 k-rings,
 *       which are hexagonal, not circular — a radius query cannot express them.</li>
 * </ul>
 * Plus a single-key entry per driver ({@code driver:location:<id>}) for the common "where is this driver"
 * lookup and for pruning stale entries.
 *
 * <p>Staleness: entries older than {@link #MAX_LOCATION_AGE_SECONDS} are ignored on read (a driver whose
 * app crashed should not keep matching), while the keys themselves expire after {@link #KEY_TTL}.
 */
public class DriverLocationCacheService implements DriverLocationCachePort {

    private static final String GEO_KEY = "driver:locations:geo";
    private static final String CELL_PREFIX = "driver:locations:h3:";
    private static final String DRIVER_KEY_PREFIX = "driver:location:";
    private static final Duration KEY_TTL = Duration.ofMinutes(10);
    private static final long MAX_LOCATION_AGE_SECONDS = 300;
    private static final int MAX_RADIUS_RESULTS = 50;

    private final CachePort cachePort;

    public DriverLocationCacheService(CachePort cachePort) {
        this.cachePort = cachePort;
    }

    @Override
    public void updateDriverLocation(UUID driverUserId, double lat, double lng, String h3Res9) {
        String member = driverUserId.toString();
        Optional<DriverCachedLocation> previous = getDriverLocation(driverUserId);
        // Drop the old cell index only when the driver crossed a cell boundary.
        previous.filter(p -> !Objects.equals(p.h3Res9(), h3Res9) && p.h3Res9() != null)
                .ifPresent(p -> cachePort.evictSet(CELL_PREFIX + p.h3Res9()));

        DriverCachedLocation location = new DriverCachedLocation(driverUserId, lat, lng, h3Res9, Instant.now());
        cachePort.put(DRIVER_KEY_PREFIX + member, location, KEY_TTL);
        cachePort.geoAdd(GEO_KEY, lng, lat, member);
        cachePort.addToSet(CELL_PREFIX + h3Res9, member, KEY_TTL);
    }

    @Override
    public Optional<DriverCachedLocation> getDriverLocation(UUID driverUserId) {
        return cachePort.get(DRIVER_KEY_PREFIX + driverUserId, DriverCachedLocation.class);
    }

    @Override
    public List<DriverCachedLocation> getDriversInRadius(double centerLat, double centerLng, double radiusKm) {
        Map<String, Double> members = cachePort.geoRadius(GEO_KEY, centerLng, centerLat, radiusKm, MAX_RADIUS_RESULTS);
        return freshLocations(members.keySet());
    }

    @Override
    public List<DriverCachedLocation> getDriversInH3Cells(Set<String> h3Cells) {
        if (h3Cells == null || h3Cells.isEmpty()) {
            return List.of();
        }
        Set<String> members = new LinkedHashSet<>();
        for (String cell : h3Cells) {
            members.addAll(cachePort.getSet(CELL_PREFIX + cell));
        }
        return freshLocations(members);
    }

    @Override
    public void removeDriver(UUID driverUserId) {
        String member = driverUserId.toString();
        getDriverLocation(driverUserId)
                .map(DriverCachedLocation::h3Res9)
                .ifPresent(cell -> cachePort.evictSet(CELL_PREFIX + cell));
        cachePort.geoRemove(GEO_KEY, member);
        cachePort.evict(DRIVER_KEY_PREFIX + member);
    }

    private List<DriverCachedLocation> freshLocations(Collection<String> members) {
        Instant now = Instant.now();
        List<DriverCachedLocation> result = new ArrayList<>(members.size());
        for (String member : members) {
            Optional<DriverCachedLocation> location = cachePort.get(DRIVER_KEY_PREFIX + member, DriverCachedLocation.class);
            if (location.isPresent() && !location.get().isStale(now, MAX_LOCATION_AGE_SECONDS)) {
                result.add(location.get());
            }
            // Stale or missing entries are left in place; the key TTL prunes them.
        }
        return result;
    }
}
