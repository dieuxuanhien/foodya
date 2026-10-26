package com.foodya.backend.application.ports.out;

import com.foodya.backend.domain.value_objects.LatLng;
import com.foodya.backend.domain.value_objects.RouteResult;

import java.util.List;

public interface RouteCalculationPort {

    /** Route visiting the waypoints in exactly the given order. */
    RouteResult calculateRoute(LatLng origin, LatLng destination, List<LatLng> waypoints);

    /** Route where the provider may reorder the waypoints to minimise total distance. */
    RouteResult calculateOptimizedRoute(LatLng origin, LatLng destination, List<LatLng> waypoints);
}
