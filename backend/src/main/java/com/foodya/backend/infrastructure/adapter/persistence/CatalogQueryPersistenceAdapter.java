package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.dto.MenuItemModel;
import com.foodya.backend.application.dto.RestaurantModel;
import com.foodya.backend.application.ports.out.CatalogQueryPort;
import com.foodya.backend.domain.value_objects.RestaurantStatus;
import com.foodya.backend.infrastructure.adapter.mapper.CatalogPersistenceMapper;
import com.foodya.backend.infrastructure.repository.MenuItemRepository;
import com.foodya.backend.infrastructure.repository.RestaurantRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CatalogQueryPersistenceAdapter implements CatalogQueryPort {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;

    public CatalogQueryPersistenceAdapter(RestaurantRepository restaurantRepository,
                                          MenuItemRepository menuItemRepository) {
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @Override
    public List<RestaurantModel> findAllRestaurants() {
        return restaurantRepository.findAll().stream().map(CatalogPersistenceMapper::toModel).toList();
    }

    @Override
    public List<RestaurantModel> findRestaurantsByH3IndexAndStatus(Collection<String> h3Indexes,
                                                                   Collection<RestaurantStatus> statuses) {
        return restaurantRepository.findByH3IndexRes9InAndStatusIn(h3Indexes, statuses)
                .stream()
                .map(CatalogPersistenceMapper::toModel)
                .toList();
    }

    @Override
    public Optional<RestaurantModel> findRestaurantByIdAndStatusIn(UUID id, Collection<RestaurantStatus> statuses) {
        return restaurantRepository.findByIdAndStatusIn(id, statuses).map(CatalogPersistenceMapper::toModel);
    }

    @Override
    public List<MenuItemModel> findPublicMenuItemsByRestaurant(UUID restaurantId) {
        return menuItemRepository.findByRestaurantIdAndActiveTrueAndAvailableTrueAndDeletedAtIsNull(restaurantId)
                .stream()
                .map(CatalogPersistenceMapper::toModel)
                .toList();
    }

    @Override
    public List<MenuItemModel> findActiveMenuItemsByKeyword(String keyword) {
        return menuItemRepository.findByActiveTrueAndDeletedAtIsNullAndNameContainingIgnoreCase(keyword)
                .stream()
                .map(CatalogPersistenceMapper::toModel)
                .toList();
    }
}