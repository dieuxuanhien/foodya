package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.ports.out.RestaurantPort;
import com.foodya.backend.domain.entities.Restaurant;
import com.foodya.backend.domain.value_objects.RestaurantStatus;
import com.foodya.backend.infrastructure.repository.RestaurantRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class RestaurantPersistenceAdapter implements RestaurantPort {

    private final RestaurantRepository repository;

    public RestaurantPersistenceAdapter(RestaurantRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Restaurant> findAll() {
        return repository.findAll();
    }

    @Override
    public List<Restaurant> findByH3IndexRes9InAndStatusIn(Collection<String> h3Indexes,
                                                           Collection<RestaurantStatus> statuses) {
        return repository.findByH3IndexRes9InAndStatusIn(h3Indexes, statuses);
    }

    @Override
    public Optional<Restaurant> findById(UUID id) {
        return repository.findById(Objects.requireNonNull(id));
    }

    @Override
    public Optional<Restaurant> findByIdAndStatusIn(UUID id, Collection<RestaurantStatus> statuses) {
        return repository.findByIdAndStatusIn(id, statuses);
    }

    @Override
    public Restaurant save(Restaurant restaurant) {
        return repository.save(Objects.requireNonNull(restaurant));
    }
}
