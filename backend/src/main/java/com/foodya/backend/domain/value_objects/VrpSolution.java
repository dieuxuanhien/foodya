package com.foodya.backend.domain.value_objects;

import java.util.List;
import java.util.UUID;

/**
 * Solution output from VRP solver.
 */
public record VrpSolution(
        List<VrpRoute> routes,
        long totalDistance,
        long totalTime,
        long objectiveValue,
        long solverTimeMs
) {}
