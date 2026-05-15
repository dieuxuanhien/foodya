package com.foodya.backend.application.ports.in;

import com.foodya.backend.application.dto.AdminRestaurantCreateCommand;
import com.foodya.backend.application.dto.AdminRestaurantUpdateCommand;
import com.foodya.backend.application.dto.CreateMenuItemRequest;
import com.foodya.backend.application.dto.MenuCategoryData;
import com.foodya.backend.application.dto.MenuItemData;
import com.foodya.backend.application.dto.OrderData;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.dto.RestaurantData;
import com.foodya.backend.application.dto.UpdateMenuItemRequest;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.value_objects.RestaurantStatus;

import java.util.UUID;

public interface AdminGovernanceUseCase {

    PaginatedResult<RestaurantData> listRestaurants(String keyword, RestaurantStatus status, Integer page, Integer size);

    RestaurantData getRestaurantById(UUID restaurantId);

    RestaurantData createRestaurant(AdminRestaurantCreateCommand command, UUID actorId);

    RestaurantData updateRestaurant(UUID restaurantId, AdminRestaurantUpdateCommand command, UUID actorId);

    RestaurantData approveRestaurant(UUID restaurantId, UUID actorId);

    RestaurantData rejectRestaurant(UUID restaurantId, UUID actorId);

    void deleteRestaurant(UUID restaurantId, UUID actorId);

    PaginatedResult<OrderData> listOrders(OrderStatus status, Integer page, Integer size);

    PaginatedResult<MenuCategoryData> listMenuCategories(UUID restaurantId, Integer page, Integer size);

    PaginatedResult<MenuItemData> listMenuItems(UUID restaurantId, String taxonomyCode, String keyword, Integer page, Integer size);

    MenuItemData createMenuItem(UUID restaurantId, CreateMenuItemRequest request, UUID actorId);

    MenuItemData updateMenuItem(UUID menuItemId, UpdateMenuItemRequest request, UUID actorId);

    OrderData updateOrderStatus(UUID orderId, OrderStatus targetStatus, UUID actorId);

    void deleteOrder(UUID orderId, UUID actorId);

    void hardDeleteMenuItem(UUID menuItemId, UUID actorId);
}
