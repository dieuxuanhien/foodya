import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/network/api_error_ui_message.dart';
import '../../domain/repositories/customer_order_repository.dart';
import 'order_detail_state.dart';

class OrderDetailCubit extends Cubit<OrderDetailState> {
  OrderDetailCubit({required CustomerOrderRepository repository})
    : _repository = repository,
      super(const OrderDetailState.initial());

  final CustomerOrderRepository _repository;

  String? _orderId;

  Future<void> load(String orderId) async {
    if (state.isBusy) {
      return;
    }

    _orderId = orderId;
    emit(
      state.copyWith(
        status: OrderDetailStatus.loading,
        isTrackingLoading: true,
        clearError: true,
        clearInfo: true,
      ),
    );

    try {
      final detail = await _repository.getOrderDetail(orderId);
      emit(
        state.copyWith(
          status: OrderDetailStatus.success,
          order: detail,
          clearError: true,
        ),
      );
      await refreshTracking();
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to load order detail.',
      );
      emit(
        state.copyWith(
          status: OrderDetailStatus.failure,
          errorMessage: presentation.message,
        ),
      );
    }
  }

  Future<void> refreshTracking() async {
    final orderId = _orderId;
    if (orderId == null) {
      return;
    }

    emit(state.copyWith(isTrackingLoading: true));
    try {
      final points = await _repository.getTrackingPoints(orderId);
      emit(
        state.copyWith(
          trackingPoints: points,
          isTrackingLoading: false,
          clearError: true,
        ),
      );
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to load tracking updates.',
      );
      emit(
        state.copyWith(
          isTrackingLoading: false,
          errorMessage: presentation.message,
        ),
      );
    }
  }

  Future<void> cancelOrder({String? reason}) async {
    final orderId = _orderId;
    if (orderId == null || state.isBusy) {
      return;
    }

    emit(
      state.copyWith(
        status: OrderDetailStatus.cancelling,
        clearError: true,
        clearInfo: true,
      ),
    );

    try {
      final detail = await _repository.cancelOrder(orderId, reason: reason);
      emit(
        state.copyWith(
          status: OrderDetailStatus.success,
          order: detail,
          infoMessage: 'Order cancelled.',
          clearError: true,
        ),
      );
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to cancel order.',
      );
      emit(
        state.copyWith(
          status: OrderDetailStatus.failure,
          errorMessage: presentation.message,
        ),
      );
    }
  }

  void clearFeedback() {
    emit(state.copyWith(clearError: true, clearInfo: true));
  }
}
