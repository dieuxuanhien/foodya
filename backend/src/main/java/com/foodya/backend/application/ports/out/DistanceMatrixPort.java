package com.foodya.backend.application.ports.out;

import com.foodya.backend.domain.value_objects.DistanceMatrixResult;
import com.foodya.backend.domain.value_objects.LatLng;

import java.util.List;

/**
 * Outbound port for retrieving real-road distance/time matrices between locations.
 * Implemented by GoongDistanceMatrixAdapter (Goong v2 API).
 */
public interface DistanceMatrixPort {

    /**
     * Returns a time matrix (seconds) between all pairs of locations.
     */
    long[][] getTimeMatrix(List<LatLng> locations, String vehicleType);

    /**
     * Returns a distance matrix (meters) between all pairs of locations.
     */
    long[][] getDistanceMatrix(List<LatLng> locations, String vehicleType);

    /**
     * Returns both time and distance matrices.
     */
    DistanceMatrixResult getFullMatrix(List<LatLng> locations, String vehicleType);
}
