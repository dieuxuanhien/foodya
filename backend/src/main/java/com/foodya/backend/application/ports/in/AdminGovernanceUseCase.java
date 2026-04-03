package com.foodya.backend.application.ports.in;

import com.foodya.backend.application.dto.OrderModel;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.dto.RestaurantModel;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.value_objects.RestaurantStatus;

import java.util.UUID;

public interface AdminGovernanceUseCase {

    PaginatedResult<RestaurantModel> listRestaurants(String keyword, RestaurantStatus status, Integer page, Integer size);

    RestaurantModel approveRestaurant(UUID restaurantId, UUID actorId);

    RestaurantModel rejectRestaurant(UUID restaurantId, UUID actorId);

    void deleteRestaurant(UUID restaurantId, UUID actorId);

    PaginatedResult<OrderModel> listOrders(OrderStatus status, Integer page, Integer size);

    OrderModel updateOrderStatus(UUID orderId, OrderStatus targetStatus, UUID actorId);

    void deleteOrder(UUID orderId, UUID actorId);

    void hardDeleteMenuItem(UUID menuItemId, UUID actorId);
}
