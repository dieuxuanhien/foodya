package com.foodya.backend.application.ports.out;

import java.math.BigDecimal;
import java.util.Set;

public interface GeoPort {

    String h3Res9(double lat, double lng);

    Set<String> h3KRingRes9(double lat, double lng, double radiusKm);

    BigDecimal haversineKm(double lat1, double lng1, double lat2, double lng2);

    String reverseGeocodeVi(double lat, double lng);

    String geoToH3Index(double lat, double lng, int resolution);
}