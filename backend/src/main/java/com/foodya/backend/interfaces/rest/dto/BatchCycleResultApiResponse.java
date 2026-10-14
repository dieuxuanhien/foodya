package com.foodya.backend.interfaces.rest.dto;

public record BatchCycleResultApiResponse(
        int batchesCreated,
        int ordersAssigned,
        int singleAssigned,
        int droppedOrders,
        long solverTimeMs
) {}
