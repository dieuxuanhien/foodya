package com.foodya.backend.application.ports.in;

import com.foodya.backend.application.dto.CreateMenuCategoryRequest;
import com.foodya.backend.application.dto.CreateMenuItemRequest;
import com.foodya.backend.application.dto.CreateRestaurantRequest;
import com.foodya.backend.application.dto.MenuCategoryModel;
import com.foodya.backend.application.dto.MenuItemModel;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.dto.RestaurantModel;
import com.foodya.backend.application.dto.UpdateMenuCategoryRequest;
import com.foodya.backend.application.dto.UpdateMenuItemAvailabilityRequest;
import com.foodya.backend.application.dto.UpdateMenuItemRequest;
import com.foodya.backend.application.dto.UpdateRestaurantRequest;

import java.util.UUID;

public interface MerchantCatalogUseCase {

    RestaurantModel createRestaurant(UUID merchantUserId, CreateRestaurantRequest request);

    RestaurantModel updateRestaurant(UUID merchantUserId, UUID restaurantId, UpdateRestaurantRequest request);

    MenuCategoryModel createCategory(UUID merchantUserId, UUID restaurantId, CreateMenuCategoryRequest request);

    PaginatedResult<MenuCategoryModel> listCategories(UUID merchantUserId, UUID restaurantId, Integer page, Integer size);

    MenuCategoryModel updateCategory(UUID merchantUserId, UUID categoryId, UpdateMenuCategoryRequest request);

    void deleteCategory(UUID merchantUserId, UUID categoryId);

    MenuItemModel createMenuItem(UUID merchantUserId, UUID restaurantId, CreateMenuItemRequest request);

    PaginatedResult<MenuItemModel> listMenuItems(UUID merchantUserId, UUID restaurantId, Integer page, Integer size);

    MenuItemModel updateMenuItem(UUID merchantUserId, UUID menuItemId, UpdateMenuItemRequest request);

    void softDeleteMenuItem(UUID merchantUserId, UUID menuItemId);

    MenuItemModel updateAvailability(UUID merchantUserId, UUID menuItemId, UpdateMenuItemAvailabilityRequest request);
}
