package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.dto.MenuItemModel;
import com.foodya.backend.application.dto.OrderItemModel;
import com.foodya.backend.application.dto.OrderModel;
import com.foodya.backend.application.dto.OrderPaymentModel;
import com.foodya.backend.application.dto.RestaurantModel;
import com.foodya.backend.application.ports.out.OrderCheckoutPort;
import com.foodya.backend.domain.value_objects.RestaurantStatus;
import com.foodya.backend.infrastructure.mapper.CatalogPersistenceMapper;
import com.foodya.backend.infrastructure.mapper.OrderCheckoutPersistenceMapper;
import com.foodya.backend.domain.entities.Order;
import com.foodya.backend.domain.entities.OrderItem;
import com.foodya.backend.infrastructure.mapper.MenuItemMapper;
import com.foodya.backend.infrastructure.mapper.OrderItemMapper;
import com.foodya.backend.infrastructure.mapper.OrderMapper;
import com.foodya.backend.infrastructure.mapper.OrderPaymentMapper;
import com.foodya.backend.infrastructure.mapper.RestaurantMapper;
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
public class OrderCheckoutAdapter implements OrderCheckoutPort {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderPaymentRepository orderPaymentRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderPaymentMapper orderPaymentMapper;
    private final RestaurantMapper restaurantMapper;
    private final MenuItemMapper menuItemMapper;

    public OrderCheckoutAdapter(OrderRepository orderRepository,
                                          OrderItemRepository orderItemRepository,
                                          OrderPaymentRepository orderPaymentRepository,
                                          RestaurantRepository restaurantRepository,
                                          MenuItemRepository menuItemRepository,
                                          OrderMapper orderMapper,
                                          OrderItemMapper orderItemMapper,
                                          OrderPaymentMapper orderPaymentMapper,
                                          RestaurantMapper restaurantMapper,
                                          MenuItemMapper menuItemMapper) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderPaymentRepository = orderPaymentRepository;
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderPaymentMapper = orderPaymentMapper;
        this.restaurantMapper = restaurantMapper;
        this.menuItemMapper = menuItemMapper;
    }

    @Override
    public Optional<OrderModel> findByCustomerUserIdAndIdempotencyKey(UUID customerUserId, String idempotencyKey) {
        return orderRepository.findByCustomerUserIdAndIdempotencyKey(customerUserId, idempotencyKey)
                .map(orderMapper::toDomain)
                .map(OrderCheckoutPersistenceMapper::toModel);
    }

    @Override
    public Optional<RestaurantModel> findActiveRestaurantById(UUID restaurantId) {
        return restaurantRepository.findByIdAndStatusIn(restaurantId, List.of(RestaurantStatus.ACTIVE))
                .map(restaurantMapper::toDomain)
                .map(CatalogPersistenceMapper::toModel);
    }

    @Override
    public Optional<MenuItemModel> findMenuItemById(UUID menuItemId) {
        return menuItemRepository.findById(Objects.requireNonNull(menuItemId))
                .map(menuItemMapper::toDomain)
                .map(CatalogPersistenceMapper::toModel);
    }

    @Override
    @SuppressWarnings("null")
    public OrderModel saveOrder(OrderModel order) {
        OrderModel orderModel = Objects.requireNonNull(order);
        Order entity = orderModel.getId() == null
                ? new Order()
                : orderRepository.findById(Objects.requireNonNull(orderModel.getId()))
                        .map(orderMapper::toDomain)
                        .orElseGet(Order::new);
        OrderCheckoutPersistenceMapper.copyToEntity(orderModel, entity);
        var persistenceModel = orderMapper.toPersistence(entity);
        var saved = orderRepository.save(persistenceModel);
        var domainOrder = orderMapper.toDomain(saved);
        return OrderCheckoutPersistenceMapper.toModel(domainOrder);
    }

    @Override
    @SuppressWarnings("null")
    public void saveOrderItems(List<OrderItemModel> items) {
        List<OrderItem> entities = Objects.requireNonNull(items).stream()
                .map(OrderCheckoutPersistenceMapper::toEntity)
                .toList();
        orderItemRepository.saveAll(entities
                .stream()
                .map(orderItemMapper::toPersistence)
                .toList());
    }

    @Override
    @SuppressWarnings("null")
    public void saveOrderPayment(OrderPaymentModel payment) {
        var entity = OrderCheckoutPersistenceMapper.toEntity(Objects.requireNonNull(payment));
        orderPaymentRepository.save(orderPaymentMapper.toPersistence(Objects.requireNonNull(entity)));
    }
}