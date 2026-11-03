package com.foodya.backend.application.usecases;

import com.foodya.backend.domain.value_objects.DriverCachedLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DriverLocationCacheServiceTest {

    private InMemoryCache cachePort;
    private DriverLocationCacheService service;

    @BeforeEach
    void setUp() {
        cachePort = new InMemoryCache();
        service = new DriverLocationCacheService(cachePort);
    }

    @Test
    void updateStoresLocationGeoAndH3Index() {
        UUID driver = UUID.randomUUID();
        service.updateDriverLocation(driver, 10.762, 106.660, "cell-A");

        assertThat(service.getDriverLocation(driver)).map(DriverCachedLocation::h3Res9).contains("cell-A");
        assertThat(service.getDriversInH3Cells(Set.of("cell-A")))
                .extracting(DriverCachedLocation::driverUserId).containsExactly(driver);
        assertThat(service.getDriversInRadius(10.762, 106.660, 1.0))
                .extracting(DriverCachedLocation::driverUserId).containsExactly(driver);
    }

    @Test
    void movingToNewCellRemovesOldCellIndex() {
        UUID driver = UUID.randomUUID();
        service.updateDriverLocation(driver, 10.762, 106.660, "cell-A");
        service.updateDriverLocation(driver, 10.762, 106.660, "cell-B");

        assertThat(service.getDriversInH3Cells(Set.of("cell-A"))).isEmpty();
        assertThat(service.getDriversInH3Cells(Set.of("cell-B"))).hasSize(1);
    }

    @Test
    void removingDriverClearsAllIndexes() {
        UUID driver = UUID.randomUUID();
        service.updateDriverLocation(driver, 10.762, 106.660, "cell-A");

        service.removeDriver(driver);

        assertThat(service.getDriverLocation(driver)).isEmpty();
        assertThat(service.getDriversInH3Cells(Set.of("cell-A"))).isEmpty();
        assertThat(service.getDriversInRadius(10.762, 106.660, 1.0)).isEmpty();
    }

    @Test
    void radiusQueryReturnsOnlyDriversInsideRadius() {
        UUID near = UUID.randomUUID();
        UUID far = UUID.randomUUID();
        service.updateDriverLocation(near, 10.762, 106.660, "cell-A");
        service.updateDriverLocation(far, 10.900, 106.900, "cell-B");

        List<DriverCachedLocation> inRadius = service.getDriversInRadius(10.762, 106.660, 2.0);

        assertThat(inRadius).extracting(DriverCachedLocation::driverUserId).containsExactly(near);
    }

    @Test
    void staleEntriesAreNotReturned() {
        UUID driver = UUID.randomUUID();
        // Write a location that is ten minutes old (beyond the 5-minute freshness window).
        cachePort.put("driver:location:" + driver, new DriverCachedLocation(
                driver, 10.762, 106.660, "cell-A",
                java.time.Instant.now().minusSeconds(600)), java.time.Duration.ofMinutes(10));
        cachePort.addToSet("driver:locations:h3:cell-A", driver.toString(), java.time.Duration.ofMinutes(10));

        assertThat(service.getDriversInH3Cells(Set.of("cell-A"))).isEmpty();
    }
}
