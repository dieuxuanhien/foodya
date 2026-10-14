package com.foodya.backend.application.ports.out;

import com.foodya.backend.domain.value_objects.VrpProblem;
import com.foodya.backend.domain.value_objects.VrpSolution;

import java.util.Optional;

/**
 * Outbound port for solving the VRPPD-TW (Vehicle Routing Problem with Pickup
 * and Delivery and Time Windows). Implemented by OrToolsVrpSolverAdapter.
 */
public interface VrpSolverPort {

    /**
     * Solves the VRPPD-TW problem.
     *
     * @param problem full problem definition with matrices, orders, drivers, constraints
     * @return optimal solution, or empty if infeasible/timeout
     */
    Optional<VrpSolution> solve(VrpProblem problem);
}
