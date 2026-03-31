package com.foodya.backend.application.port.out;

import java.math.BigDecimal;

public interface RouteDistancePort {

    BigDecimal routeDistanceKm(double originLat,
                               double originLng,
                               double destinationLat,
                               double destinationLng);
}
