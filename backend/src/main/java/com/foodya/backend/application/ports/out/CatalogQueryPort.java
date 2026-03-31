package com.foodya.backend.application.ports.out;

import com.foodya.backend.application.dto.MenuItemModel;
import com.foodya.backend.application.dto.RestaurantModel;
import com.foodya.backend.domain.value_objects.RestaurantStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogQueryPort {

    List<RestaurantModel> findAllRestaurants();

    List<RestaurantModel> findRestaurantsByH3IndexAndStatus(Collection<String> h3Indexes, Collection<RestaurantStatus> statuses);

    Optional<RestaurantModel> findRestaurantByIdAndStatusIn(UUID id, Collection<RestaurantStatus> statuses);

    List<MenuItemModel> findPublicMenuItemsByRestaurant(UUID restaurantId);

    List<MenuItemModel> findActiveMenuItemsByKeyword(String keyword);
}