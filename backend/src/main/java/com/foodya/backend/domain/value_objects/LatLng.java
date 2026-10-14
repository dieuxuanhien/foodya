package com.foodya.backend.domain.value_objects;

/**
 * Represents a geographic coordinate pair (latitude, longitude).
 * Used for distance matrix computation and VRP node positioning.
 */
public record LatLng(double lat, double lng) {

    public String toGoongString() {
        return lat + "," + lng;
    }
}
