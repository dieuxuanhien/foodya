package com.foodya.backend.application.ports.out;

import com.foodya.backend.application.dto.MenuItemData;
import com.foodya.backend.application.dto.RestaurantData;
import com.foodya.backend.domain.value_objects.RestaurantStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogQueryPort {

    List<RestaurantData> findAllRestaurants();

    List<RestaurantData> findRestaurantsByH3IndexAndStatus(Collection<String> h3Indexes, Collection<RestaurantStatus> statuses);

    Optional<RestaurantData> findRestaurantByIdAndStatusIn(UUID id, Collection<RestaurantStatus> statuses);

    List<MenuItemData> findPublicMenuItemsByRestaurant(UUID restaurantId);

    List<MenuItemData> findPublicMenuItemsByRestaurant(UUID restaurantId, Collection<String> taxonomyCodes);

    List<MenuItemData> findPublicMenuItemsByTaxonomyCodes(Collection<String> taxonomyCodes);

    List<MenuItemData> findActiveMenuItemsByKeyword(String keyword);
}