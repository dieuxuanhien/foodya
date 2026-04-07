package com.foodya.backend.application.ports.in;

import com.foodya.backend.application.dto.CreateMenuCategoryRequest;
import com.foodya.backend.application.dto.CreateMenuItemRequest;
import com.foodya.backend.application.dto.CreateRestaurantRequest;
import com.foodya.backend.application.dto.MenuCategoryData;
import com.foodya.backend.application.dto.MenuItemData;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.dto.RestaurantData;
import com.foodya.backend.application.dto.UpdateMenuCategoryRequest;
import com.foodya.backend.application.dto.UpdateMenuItemAvailabilityRequest;
import com.foodya.backend.application.dto.UpdateMenuItemRequest;
import com.foodya.backend.application.dto.UpdateRestaurantRequest;

import java.util.UUID;

public interface MerchantCatalogUseCase {

    RestaurantData createRestaurant(UUID merchantUserId, CreateRestaurantRequest request);

    RestaurantData updateRestaurant(UUID merchantUserId, UUID restaurantId, UpdateRestaurantRequest request);

    RestaurantData uploadRestaurantBackgroundImage(UUID merchantUserId,
                                                   UUID restaurantId,
                                                   String originalFileName,
                                                   String contentType,
                                                   byte[] content);

    RestaurantData uploadRestaurantAvatarImage(UUID merchantUserId,
                                               UUID restaurantId,
                                               String originalFileName,
                                               String contentType,
                                               byte[] content);

    MenuCategoryData createCategory(UUID merchantUserId, UUID restaurantId, CreateMenuCategoryRequest request);

    PaginatedResult<MenuCategoryData> listCategories(UUID merchantUserId, UUID restaurantId, Integer page, Integer size);

    MenuCategoryData updateCategory(UUID merchantUserId, UUID categoryId, UpdateMenuCategoryRequest request);

    void deleteCategory(UUID merchantUserId, UUID categoryId);

    MenuItemData createMenuItem(UUID merchantUserId, UUID restaurantId, CreateMenuItemRequest request);

    PaginatedResult<MenuItemData> listMenuItems(UUID merchantUserId, UUID restaurantId, Integer page, Integer size);

    MenuItemData updateMenuItem(UUID merchantUserId, UUID menuItemId, UpdateMenuItemRequest request);

    void softDeleteMenuItem(UUID merchantUserId, UUID menuItemId);

    MenuItemData updateAvailability(UUID merchantUserId, UUID menuItemId, UpdateMenuItemAvailabilityRequest request);

    MenuItemData uploadMenuItemImage(UUID merchantUserId,
                                     UUID menuItemId,
                                     String originalFileName,
                                     String contentType,
                                     byte[] content);
}
