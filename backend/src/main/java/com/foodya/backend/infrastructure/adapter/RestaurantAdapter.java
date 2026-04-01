package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.ports.out.RestaurantPort;
import com.foodya.backend.domain.entities.Restaurant;
import com.foodya.backend.domain.value_objects.RestaurantStatus;
import com.foodya.backend.infrastructure.mapper.RestaurantMapper;
import com.foodya.backend.infrastructure.repository.RestaurantRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class RestaurantAdapter implements RestaurantPort {

    private final RestaurantRepository repository;
    private final RestaurantMapper mapper;

    public RestaurantAdapter(RestaurantRepository repository, RestaurantMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Restaurant> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Restaurant> findByH3IndexRes9InAndStatusIn(Collection<String> h3Indexes,
                                                           Collection<RestaurantStatus> statuses) {
        return repository.findByH3IndexRes9InAndStatusIn(h3Indexes, statuses)
            .stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public Optional<Restaurant> findById(UUID id) {
        return repository.findById(Objects.requireNonNull(id)).map(mapper::toDomain);
    }

    @Override
    public Optional<Restaurant> findByIdAndStatusIn(UUID id, Collection<RestaurantStatus> statuses) {
        return repository.findByIdAndStatusIn(id, statuses).map(mapper::toDomain);
    }

    @Override
    @SuppressWarnings("null")
    public Restaurant save(Restaurant restaurant) {
        var saved = repository.save(mapper.toPersistence(Objects.requireNonNull(restaurant)));
        return mapper.toDomain(saved);
    }
}
