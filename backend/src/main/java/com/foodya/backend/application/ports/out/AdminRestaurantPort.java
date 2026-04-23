package com.foodya.backend.application.ports.out;

import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.value_objects.RestaurantStatus;
import com.foodya.backend.domain.entities.Restaurant;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface AdminRestaurantPort {

    PaginatedResult<Restaurant> search(String keyword, RestaurantStatus status, int page, int size);

    Optional<Restaurant> findById(UUID restaurantId);

    Restaurant save(Restaurant restaurant);

    void delete(Restaurant restaurant);

    boolean hasOrdersInStatuses(UUID restaurantId, Collection<OrderStatus> statuses);
}
