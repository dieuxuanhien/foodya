package com.foodya.backend.application.ports.out;

import com.foodya.backend.domain.entities.Restaurant;
import com.foodya.backend.domain.value_objects.RestaurantStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RestaurantPort {

    List<Restaurant> findAll();

    List<Restaurant> findByH3IndexRes9InAndStatusIn(Collection<String> h3Indexes, Collection<RestaurantStatus> statuses);

    Optional<Restaurant> findById(UUID id);

    Optional<Restaurant> findByIdAndStatusIn(UUID id, Collection<RestaurantStatus> statuses);

    Restaurant save(Restaurant restaurant);
}
