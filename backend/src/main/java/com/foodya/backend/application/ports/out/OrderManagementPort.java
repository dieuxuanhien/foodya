package com.foodya.backend.application.ports.out;

import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.entities.Order;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderManagementPort {

    Optional<Order> findById(UUID orderId);

    PaginatedResult<Order> findByCustomerUserIdOrderByPlacedAtDesc(UUID customerUserId, int page, int size);

    PaginatedResult<Order> findByRestaurantIdOrderByPlacedAtDesc(UUID restaurantId, int page, int size);

    List<Order> findByStatusInOrderByPlacedAtAsc(Collection<OrderStatus> statuses);

    /**
     * Finds orders eligible for batching: status = ACCEPTED, no driver assigned, no batch assigned.
     */
    List<Order> findEligibleForBatching();

    /** Orders sitting in the given status placed before {@code before} (timeout sweeps). */
    List<Order> findStaleByStatus(OrderStatus status, java.time.OffsetDateTime before);

    /** Orders a driver currently holds (ASSIGNED, PREPARING or DELIVERING), oldest first. */
    List<Order> findOpenByDriverUserId(UUID driverUserId);

    /** Successful deliveries credited to a driver within the window. */
    List<Order> findCompletedByDriverUserId(UUID driverUserId,
                                            java.time.OffsetDateTime from,
                                            java.time.OffsetDateTime to);


    Order save(Order order);
}
