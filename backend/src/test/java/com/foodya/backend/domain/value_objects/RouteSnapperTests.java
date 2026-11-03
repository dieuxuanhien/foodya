package com.foodya.backend.domain.value_objects;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RouteSnapperTests {

    // A straight-ish 3-point route running north along lng 106.66.
    private static final List<LatLng> ROUTE = List.of(
            new LatLng(10.760, 106.660),
            new LatLng(10.770, 106.660),
            new LatLng(10.780, 106.660));
    private static final String ENCODED = PolylineCodec.encode(ROUTE);
    private static final int DURATION = 600;

    @Test
    void polylineRoundTrips() {
        List<LatLng> decoded = PolylineCodec.decode(ENCODED);
        assertEquals(ROUTE.size(), decoded.size());
        for (int i = 0; i < ROUTE.size(); i++) {
            assertEquals(ROUTE.get(i).lat(), decoded.get(i).lat(), 1e-5);
            assertEquals(ROUTE.get(i).lng(), decoded.get(i).lng(), 1e-5);
        }
    }

    @Test
    void snapsPointNearRouteOntoPolyline() {
        // 20 m east of the midpoint of the first segment.
        SnappedPosition snapped = RouteSnapper.snap(ENCODED, DURATION, 10.765, 106.6602);
        assertNotNull(snapped);
        assertEquals(106.660, snapped.lng(), 1e-4);
        assertEquals(10.765, snapped.lat(), 1e-4);
        assertEquals(0.25, snapped.progress(), 0.02);
    }

    @Test
    void progressIncreasesAlongRouteAndEndsAtZeroRemaining() {
        SnappedPosition start = RouteSnapper.snap(ENCODED, DURATION, 10.760, 106.660);
        SnappedPosition end = RouteSnapper.snap(ENCODED, DURATION, 10.780, 106.660);
        assertNotNull(start);
        assertNotNull(end);
        assertEquals(0.0, start.progress(), 0.001);
        assertEquals(1.0, end.progress(), 0.001);
        assertEquals(0, end.remainingDistanceMeters());
        assertEquals(0, end.remainingDurationSeconds());
        // ~2224 m total for 0.02 degrees of latitude.
        assertTrue(start.remainingDistanceMeters() > 2100 && start.remainingDistanceMeters() < 2300);
    }

    @Test
    void clampsPointBeyondRouteEnd() {
        SnappedPosition beyond = RouteSnapper.snap(ENCODED, DURATION, 10.800, 106.660);
        assertNotNull(beyond);
        assertEquals(1.0, beyond.progress(), 0.001);
    }

    @Test
    void returnsNullForUnusablePolyline() {
        assertNull(RouteSnapper.snap(null, DURATION, 10.76, 106.66));
        assertNull(RouteSnapper.snap("", DURATION, 10.76, 106.66));
        assertNull(RouteSnapper.snap(PolylineCodec.encode(List.of(new LatLng(10.76, 106.66))), DURATION, 10.76, 106.66));
    }
}
