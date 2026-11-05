package com.foodya.backend.application.ports.out;

import com.foodya.backend.domain.value_objects.DriverCachedLocation;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface DriverLocationCachePort {

    void updateDriverLocation(UUID driverUserId, double lat, double lng, String h3Res9);

    Optional<DriverCachedLocation> getDriverLocation(UUID driverUserId);

    List<DriverCachedLocation> getDriversInRadius(double centerLat, double centerLng, double radiusKm);

    List<DriverCachedLocation> getDriversInH3Cells(Set<String> h3Cells);

    void removeDriver(UUID driverUserId);
}
