import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/network/api_error_ui_message.dart';
import '../../domain/models/merchant_restaurant.dart';
import '../../domain/repositories/merchant_order_repository.dart';
import '../../domain/repositories/merchant_restaurant_repository.dart';
import '../../domain/repositories/merchant_revenue_repository.dart';
import 'merchant_home_state.dart';
import 'merchant_restaurant_selection_cubit.dart';

class MerchantHomeCubit extends Cubit<MerchantHomeState> {
  MerchantHomeCubit({
    required MerchantRestaurantRepository restaurantRepository,
    required MerchantOrderRepository orderRepository,
    required MerchantRevenueRepository revenueRepository,
    MerchantRestaurantSelectionCubit? selectionCubit,
  }) : _restaurantRepository = restaurantRepository,
       _orderRepository = orderRepository,
       _revenueRepository = revenueRepository,
       _selectionCubit = selectionCubit,
       super(const MerchantHomeState.initial());

  final MerchantRestaurantRepository _restaurantRepository;
  final MerchantOrderRepository _orderRepository;
  final MerchantRevenueRepository _revenueRepository;
  final MerchantRestaurantSelectionCubit? _selectionCubit;

  Future<void> load() async {
    if (state.isLoading) {
      return;
    }

    emit(state.copyWith(status: MerchantHomeStatus.loading, clearError: true));

    try {
      final restaurants = await _restaurantRepository.listRestaurants();
      final selected = _resolveSelectedRestaurant(restaurants);

      if (selected == null) {
        _selectionCubit?.selectRestaurantId(null);
        emit(
          state.copyWith(
            status: MerchantHomeStatus.success,
            restaurants: restaurants,
            orders: const [],
            clearSelectedRestaurant: true,
            clearRevenueReport: true,
            clearError: true,
          ),
        );
        return;
      }

      await _loadDashboardFor(restaurants: restaurants, selected: selected);
    } catch (error) {
      _emitFailure(error);
    }
  }

  Future<void> selectRestaurant(MerchantRestaurant restaurant) async {
    if (state.isLoading) {
      return;
    }
    await _loadDashboardFor(
      restaurants: state.restaurants,
      selected: restaurant,
    );
  }

  Future<void> _loadDashboardFor({
    required List<MerchantRestaurant> restaurants,
    required MerchantRestaurant selected,
  }) async {
    _selectionCubit?.selectRestaurant(selected);
    emit(
      state.copyWith(
        status: MerchantHomeStatus.loading,
        restaurants: restaurants,
        selectedRestaurant: selected,
        clearError: true,
      ),
    );

    try {
      final to = DateTime.now();
      final from = to.subtract(const Duration(days: 7));
      final orders = await _orderRepository.listRestaurantOrders(selected.id);
      final revenue = await _revenueRepository.getRevenueReport(
        from: from,
        to: to,
        topItems: 3,
      );

      emit(
        state.copyWith(
          status: MerchantHomeStatus.success,
          restaurants: restaurants,
          selectedRestaurant: selected,
          orders: orders,
          revenueReport: revenue,
          clearError: true,
        ),
      );
    } catch (error) {
      _emitFailure(error);
    }
  }

  MerchantRestaurant? _resolveSelectedRestaurant(
    List<MerchantRestaurant> restaurants,
  ) {
    if (restaurants.isEmpty) {
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
    return restaurants.first;
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

  void _emitFailure(Object error) {
    final presentation = ApiErrorUiMessageMapper.mapAny(
      error,
      fallback: 'Unable to load merchant dashboard.',
    );
    emit(
      state.copyWith(
        status: MerchantHomeStatus.failure,
        errorMessage: presentation.message,
      ),
    );
  }
}
