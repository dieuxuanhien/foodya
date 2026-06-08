import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/network/api_error_ui_message.dart';
import '../../domain/repositories/customer_order_repository.dart';
import 'order_list_state.dart';

class OrderListCubit extends Cubit<OrderListState> {
  OrderListCubit({required CustomerOrderRepository repository})
    : _repository = repository,
      super(const OrderListState.initial());

  final CustomerOrderRepository _repository;

  Future<void> loadOrders() async {
    if (state.isBusy) {
      return;
    }

    emit(state.copyWith(status: OrderListStatus.loading, clearError: true));

    try {
      final orders = await _repository.listOrders();
      emit(
        state.copyWith(
          status:
              orders.isEmpty ? OrderListStatus.empty : OrderListStatus.success,
          orders: orders,
          clearError: true,
        ),
      );
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to load your orders.',
      );
      emit(
        state.copyWith(
          status: OrderListStatus.failure,
          errorMessage: presentation.message,
        ),
      );
    }
  }
}
