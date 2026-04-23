package com.foodya.backend.application.ports.in;

import com.foodya.backend.application.dto.OrderData;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.dto.RestaurantData;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.value_objects.RestaurantStatus;

import java.util.UUID;

public interface AdminGovernanceUseCase {

    PaginatedResult<RestaurantData> listRestaurants(String keyword, RestaurantStatus status, Integer page, Integer size);

    RestaurantData approveRestaurant(UUID restaurantId, UUID actorId);

    RestaurantData rejectRestaurant(UUID restaurantId, UUID actorId);

    void deleteRestaurant(UUID restaurantId, UUID actorId);

    PaginatedResult<OrderData> listOrders(OrderStatus status, Integer page, Integer size);

    OrderData updateOrderStatus(UUID orderId, OrderStatus targetStatus, UUID actorId);

    void deleteOrder(UUID orderId, UUID actorId);

    void hardDeleteMenuItem(UUID menuItemId, UUID actorId);
}
