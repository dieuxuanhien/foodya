package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.ports.out.DriverOnlineSessionPort;
import com.foodya.backend.application.ports.out.GeoPort;
import com.foodya.backend.domain.entities.DriverOnlineSession;
import com.foodya.backend.domain.value_objects.DriverMatchRequest;
import com.foodya.backend.domain.value_objects.DriverMatchResult;
import com.foodya.backend.domain.value_objects.DriverSessionStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DriverMatchingAdapterTest {

    private DriverOnlineSessionPort driverOnlineSessionPort;
    private GeoPort geoPort;
    private DriverMatchingAdapter adapter;

    @BeforeEach
    void setUp() {
        driverOnlineSessionPort = mock(DriverOnlineSessionPort.class);
        geoPort = mock(GeoPort.class);
        adapter = new DriverMatchingAdapter(driverOnlineSessionPort, geoPort);
    }

    @Test
    @DisplayName("Should return empty when no cells returned by geoPort")
    void testNoCells() {
        DriverMatchRequest request = new DriverMatchRequest(10.776, 106.700, 10.780, 106.710, BigDecimal.valueOf(50000));
        when(geoPort.h3KRingRes9(anyDouble(), anyDouble(), anyDouble())).thenReturn(Collections.emptySet());

        Optional<DriverMatchResult> result = adapter.findBestDriver(request);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return best driver matching request based on score")
    void testFindBestDriver() {
        DriverMatchRequest request = new DriverMatchRequest(10.776, 106.700, 10.780, 106.710, BigDecimal.valueOf(50000));
        Set<String> cells = Set.of("8928308280fffff");

        when(geoPort.h3KRingRes9(10.776, 106.700, 1.0)).thenReturn(cells);

        UUID driverId1 = UUID.randomUUID();
        DriverOnlineSession driver1 = new DriverOnlineSession();
        driver1.setId(UUID.randomUUID());
        driver1.setDriverUserId(driverId1);
        driver1.setCurrentLat(10.777);
        driver1.setCurrentLng(106.701);
        driver1.setStatus(DriverSessionStatus.ONLINE);
        driver1.setMaxConcurrentOrders(2);
        driver1.setActiveOrderCount(0);
        driver1.setLastHeartbeat(Instant.now());

        UUID driverId2 = UUID.randomUUID();
        DriverOnlineSession driver2 = new DriverOnlineSession();
        driver2.setId(UUID.randomUUID());
        driver2.setDriverUserId(driverId2);
        driver2.setCurrentLat(10.780);
        driver2.setCurrentLng(106.705);
        driver2.setStatus(DriverSessionStatus.ONLINE);
        driver2.setMaxConcurrentOrders(2);
        driver2.setActiveOrderCount(1);
        driver2.setLastHeartbeat(Instant.now());

        when(driverOnlineSessionPort.findOnlineDriversInH3Cells(cells)).thenReturn(List.of(driver1, driver2));
        when(geoPort.haversineKm(eq(10.777), eq(106.701), eq(10.776), eq(106.700))).thenReturn(BigDecimal.valueOf(0.2));
        when(geoPort.haversineKm(eq(10.780), eq(106.705), eq(10.776), eq(106.700))).thenReturn(BigDecimal.valueOf(1.5));

        Optional<DriverMatchResult> result = adapter.findBestDriver(request);

        assertThat(result).isPresent();
        assertThat(result.get().driverUserId()).isEqualTo(driverId1);
        assertThat(result.get().distanceKm()).isEqualTo(0.2);
    }
}
