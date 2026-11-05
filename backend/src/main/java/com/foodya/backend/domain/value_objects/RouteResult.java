package com.foodya.backend.domain.value_objects;

import java.util.List;

public record RouteResult(String encodedPolyline, int distanceMeters, int durationSeconds, List<RouteLeg> legs) {}
