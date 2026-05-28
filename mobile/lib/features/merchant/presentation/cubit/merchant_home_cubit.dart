import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/network/api_error_ui_message.dart';
import '../../domain/repositories/merchant_order_repository.dart';
import '../../domain/repositories/merchant_restaurant_repository.dart';
import '../../domain/repositories/merchant_revenue_repository.dart';
import 'merchant_home_state.dart';

class MerchantHomeCubit extends Cubit<MerchantHomeState> {
  MerchantHomeCubit({
    required MerchantRestaurantRepository restaurantRepository,
    required MerchantOrderRepository orderRepository,
    required MerchantRevenueRepository revenueRepository,
  }) : _restaurantRepository = restaurantRepository,
       _orderRepository = orderRepository,
       _revenueRepository = revenueRepository,
       super(const MerchantHomeState.initial());

  final MerchantRestaurantRepository _restaurantRepository;
  final MerchantOrderRepository _orderRepository;
  final MerchantRevenueRepository _revenueRepository;

  Future<void> load() async {
    if (state.isLoading) {
      return;
    }

    emit(state.copyWith(status: MerchantHomeStatus.loading, clearError: true));

    try {
      final restaurants = await _restaurantRepository.listRestaurants();
      final selected = restaurants.isEmpty ? null : restaurants.first;

      if (selected == null) {
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
}
