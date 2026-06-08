import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/network/api_error_ui_message.dart';
import '../../domain/models/merchant_order_detail.dart';
import '../../domain/models/merchant_order_summary.dart';
import '../../domain/models/merchant_restaurant.dart';
import '../../domain/repositories/merchant_order_repository.dart';
import '../../domain/repositories/merchant_restaurant_repository.dart';
import 'merchant_orders_state.dart';
import 'merchant_restaurant_selection_cubit.dart';

class MerchantOrdersCubit extends Cubit<MerchantOrdersState> {
  MerchantOrdersCubit({
    required MerchantOrderRepository orderRepository,
    required MerchantRestaurantRepository restaurantRepository,
    MerchantRestaurantSelectionCubit? selectionCubit,
  }) : _orderRepository = orderRepository,
       _restaurantRepository = restaurantRepository,
       _selectionCubit = selectionCubit,
       super(const MerchantOrdersState.initial());

  final MerchantOrderRepository _orderRepository;
  final MerchantRestaurantRepository _restaurantRepository;
  final MerchantRestaurantSelectionCubit? _selectionCubit;

  Future<void> load() async {
    if (state.isBusy) {
      return;
    }
    emit(
      state.copyWith(
        status: MerchantOrdersStatus.loading,
        clearError: true,
        clearInfo: true,
      ),
    );
    try {
      final restaurants = await _restaurantRepository.listRestaurants();
      final selected = _resolveSelectedRestaurant(restaurants);
      emit(
        state.copyWith(
          status: MerchantOrdersStatus.success,
          restaurants: restaurants,
          selectedRestaurant: selected,
          clearError: true,
        ),
      );
      if (selected != null) {
        await loadRestaurantOrders(selected, clearSelection: true);
      }
    } catch (error) {
      _emitFailure(error, 'Unable to load merchant orders.');
    }
  }

  Future<void> loadRestaurantOrders(
    MerchantRestaurant restaurant, {
    bool clearSelection = false,
  }) async {
    if (state.isActing) {
      return;
    }
    _selectionCubit?.selectRestaurant(restaurant);
    emit(
      state.copyWith(
        status: MerchantOrdersStatus.loading,
        selectedRestaurant: restaurant,
        selectedOrder: clearSelection ? null : state.selectedOrder,
        clearError: true,
        clearInfo: true,
      ),
    );
    try {
      final orders = await _orderRepository.listRestaurantOrders(restaurant.id);
      emit(
        state.copyWith(
          status: MerchantOrdersStatus.success,
          orders: orders,
          selectedOrder: clearSelection ? null : state.selectedOrder,
          clearError: true,
        ),
      );
    } catch (error) {
      _emitFailure(error, 'Unable to load restaurant orders.');
    }
  }

  Future<void> selectOrder(MerchantOrderSummary order) async {
    if (state.isBusy) {
      return;
    }
    emit(
      state.copyWith(status: MerchantOrdersStatus.loading, clearError: true),
    );
    try {
      final detail = await _orderRepository.getOrderDetail(order.orderId);
      emit(
        state.copyWith(
          status: MerchantOrdersStatus.success,
          selectedOrder: detail,
          clearError: true,
        ),
      );
    } catch (error) {
      _emitFailure(error, 'Unable to load order detail.');
    }
  }

  Future<void> updateStatus(String status) async {
    final order = state.selectedOrder;
    if (order == null || state.isActing) {
      return;
    }
    emit(
      state.copyWith(
        status: MerchantOrdersStatus.acting,
        clearError: true,
        clearInfo: true,
      ),
    );
    try {
      final detail = await _orderRepository.updateOrderStatus(
        orderId: order.orderId,
        status: status,
      );
      emit(
        state.copyWith(
          status: MerchantOrdersStatus.success,
          selectedOrder: detail,
          orders: _upsertSummary(state.orders, detail),
          infoMessage: 'Order moved to ${detail.status}.',
          clearError: true,
        ),
      );
    } catch (error) {
      _emitFailure(error, 'Unable to update order status.');
    }
  }

  void clearFeedback() {
    emit(state.copyWith(clearError: true, clearInfo: true));
  }

  void _emitFailure(Object error, String fallback) {
    final presentation = ApiErrorUiMessageMapper.mapAny(
      error,
      fallback: fallback,
    );
    emit(
      state.copyWith(
        status: MerchantOrdersStatus.failure,
        errorMessage: presentation.message,
      ),
    );
  }

  List<MerchantOrderSummary> _upsertSummary(
    List<MerchantOrderSummary> orders,
    MerchantOrderDetail detail,
  ) {
    final summary = MerchantOrderSummary(
      orderId: detail.orderId,
      orderCode: detail.orderCode,
      customerName: detail.customerName,
      restaurantName: detail.restaurantName,
      status: detail.status,
      paymentStatus: detail.paymentStatus,
      totalAmount: detail.totalAmount,
    );
    final index = orders.indexWhere((order) => order.orderId == detail.orderId);
    if (index == -1) {
      return [summary, ...orders];
    }
    return [
      for (var i = 0; i < orders.length; i++)
        if (i == index) summary else orders[i],
    ];
  }

  MerchantRestaurant? _resolveSelectedRestaurant(
    List<MerchantRestaurant> restaurants,
  ) {
    if (restaurants.isEmpty) {
      _selectionCubit?.selectRestaurantId(null);
      return null;
    }
    final selectedId = _selectionCubit?.state.restaurantId;
    if (selectedId != null) {
      final selected = _findRestaurant(restaurants, selectedId);
      if (selected != null) {
        return selected;
      }
    }
    final current = state.selectedRestaurant;
    if (current != null) {
      final selected = _findRestaurant(restaurants, current.id);
      if (selected != null) {
        return selected;
      }
    }
    final selected = restaurants.first;
    _selectionCubit?.selectRestaurant(selected);
    return selected;
  }

  MerchantRestaurant? _findRestaurant(
    List<MerchantRestaurant> restaurants,
    String restaurantId,
  ) {
    for (final restaurant in restaurants) {
      if (restaurant.id == restaurantId) {
        return restaurant;
      }
    }
    return null;
  }
}
