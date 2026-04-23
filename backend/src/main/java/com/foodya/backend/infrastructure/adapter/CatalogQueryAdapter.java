package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.dto.MenuItemData;
import com.foodya.backend.application.dto.RestaurantData;
import com.foodya.backend.application.ports.out.CatalogQueryPort;
import com.foodya.backend.domain.value_objects.RestaurantStatus;
import com.foodya.backend.infrastructure.mapper.CatalogPersistenceMapper;
import com.foodya.backend.infrastructure.mapper.MenuItemMapper;
import com.foodya.backend.infrastructure.mapper.RestaurantMapper;
import com.foodya.backend.infrastructure.repository.MenuItemRepository;
import com.foodya.backend.infrastructure.repository.RestaurantRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CatalogQueryAdapter implements CatalogQueryPort {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final RestaurantMapper restaurantMapper;
    private final MenuItemMapper menuItemMapper;

    public CatalogQueryAdapter(RestaurantRepository restaurantRepository,
                                          MenuItemRepository menuItemRepository,
                                          RestaurantMapper restaurantMapper,
                                          MenuItemMapper menuItemMapper) {
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
        this.restaurantMapper = restaurantMapper;
        this.menuItemMapper = menuItemMapper;
    }

    @Override
    public List<RestaurantData> findAllRestaurants() {
        return restaurantRepository.findAll().stream()
                .map(restaurantMapper::toDomain)
                .map(CatalogPersistenceMapper::toData)
                .toList();
    }

    @Override
    public List<RestaurantData> findRestaurantsByH3IndexAndStatus(Collection<String> h3Indexes,
                                                                   Collection<RestaurantStatus> statuses) {
        return restaurantRepository.findByH3IndexRes9InAndStatusIn(h3Indexes, statuses)
                .stream()
            .map(restaurantMapper::toDomain)
                .map(CatalogPersistenceMapper::toData)
                .toList();
    }

    @Override
    public Optional<RestaurantData> findRestaurantByIdAndStatusIn(UUID id, Collection<RestaurantStatus> statuses) {
        return restaurantRepository.findByIdAndStatusIn(id, statuses)
                .map(restaurantMapper::toDomain)
                .map(CatalogPersistenceMapper::toData);
    }

    @Override
    public List<MenuItemData> findPublicMenuItemsByRestaurant(UUID restaurantId) {
        return menuItemRepository.findPublicMenuItemsByRestaurant(restaurantId)
                .stream()
            .map(menuItemMapper::toDomain)
                .map(CatalogPersistenceMapper::toData)
                .toList();
    }

    @Override
    public List<MenuItemData> findPublicMenuItemsByRestaurant(UUID restaurantId, Collection<String> taxonomyCodes) {
        return menuItemRepository.findPublicMenuItemsByRestaurantAndTaxonomyCodes(restaurantId, taxonomyCodes)
                .stream()
            .map(menuItemMapper::toDomain)
                .map(CatalogPersistenceMapper::toData)
                .toList();
    }

    @Override
    public List<MenuItemData> findPublicMenuItemsByTaxonomyCodes(Collection<String> taxonomyCodes) {
        return menuItemRepository.findPublicMenuItemsByTaxonomyCodes(taxonomyCodes)
                .stream()
            .map(menuItemMapper::toDomain)
                .map(CatalogPersistenceMapper::toData)
                .toList();
    }

    @Override
    public List<MenuItemData> findActiveMenuItemsByKeyword(String keyword) {
        return menuItemRepository.findActiveMenuItemsByKeyword(keyword)
                .stream()
            .map(menuItemMapper::toDomain)
                .map(CatalogPersistenceMapper::toData)
                .toList();
    }
}