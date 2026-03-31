package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.dto.MenuItemModel;
import com.foodya.backend.application.dto.OrderItemModel;
import com.foodya.backend.application.dto.OrderModel;
import com.foodya.backend.application.dto.OrderPaymentModel;
import com.foodya.backend.application.dto.RestaurantModel;
import com.foodya.backend.application.ports.out.OrderCheckoutPort;
import com.foodya.backend.domain.value_objects.RestaurantStatus;
import com.foodya.backend.infrastructure.adapter.mapper.CatalogPersistenceMapper;
import com.foodya.backend.infrastructure.adapter.mapper.OrderCheckoutPersistenceMapper;
import com.foodya.backend.domain.entities.Order;
import com.foodya.backend.domain.entities.OrderItem;
import com.foodya.backend.infrastructure.repository.MenuItemRepository;
import com.foodya.backend.infrastructure.repository.OrderItemRepository;
import com.foodya.backend.infrastructure.repository.OrderPaymentRepository;
import com.foodya.backend.infrastructure.repository.OrderRepository;
import com.foodya.backend.infrastructure.repository.RestaurantRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class OrderCheckoutPersistenceAdapter implements OrderCheckoutPort {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderPaymentRepository orderPaymentRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;

    public OrderCheckoutPersistenceAdapter(OrderRepository orderRepository,
                                          OrderItemRepository orderItemRepository,
                                          OrderPaymentRepository orderPaymentRepository,
                                          RestaurantRepository restaurantRepository,
                                          MenuItemRepository menuItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderPaymentRepository = orderPaymentRepository;
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @Override
    public Optional<OrderModel> findByCustomerUserIdAndIdempotencyKey(UUID customerUserId, String idempotencyKey) {
        return orderRepository.findByCustomerUserIdAndIdempotencyKey(customerUserId, idempotencyKey)
                .map(OrderCheckoutPersistenceMapper::toModel);
    }

    @Override
    public Optional<RestaurantModel> findActiveRestaurantById(UUID restaurantId) {
        return restaurantRepository.findByIdAndStatusIn(restaurantId, List.of(RestaurantStatus.ACTIVE))
                .map(CatalogPersistenceMapper::toModel);
    }

    @Override
    public Optional<MenuItemModel> findMenuItemById(UUID menuItemId) {
        return menuItemRepository.findById(Objects.requireNonNull(menuItemId)).map(CatalogPersistenceMapper::toModel);
    }

    @Override
    public OrderModel saveOrder(OrderModel order) {
        OrderModel orderModel = Objects.requireNonNull(order);
        Order entity = orderModel.getId() == null
                ? new Order()
            : orderRepository.findById(Objects.requireNonNull(orderModel.getId())).orElseGet(Order::new);
        OrderCheckoutPersistenceMapper.copyToEntity(orderModel, entity);
        return OrderCheckoutPersistenceMapper.toModel(orderRepository.save(Objects.requireNonNull(entity)));
    }

    @Override
    public void saveOrderItems(List<OrderItemModel> items) {
        Iterable<OrderItem> entities = Objects.requireNonNull(items).stream()
                .map(OrderCheckoutPersistenceMapper::toEntity)
                .toList();
        orderItemRepository.saveAll(Objects.requireNonNull(entities));
    }

    @Override
    public void saveOrderPayment(OrderPaymentModel payment) {
        orderPaymentRepository.save(Objects.requireNonNull(OrderCheckoutPersistenceMapper.toEntity(Objects.requireNonNull(payment))));
    }
}