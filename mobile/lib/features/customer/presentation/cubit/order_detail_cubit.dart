import 'dart:async';

import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/network/api_error_ui_message.dart';
import '../../domain/repositories/customer_order_repository.dart';
import 'order_detail_state.dart';

class OrderDetailCubit extends Cubit<OrderDetailState> {
  OrderDetailCubit({required CustomerOrderRepository repository})
    : _repository = repository,
      super(const OrderDetailState.initial());

  final CustomerOrderRepository _repository;

  static const Duration _liveTrackingInterval = Duration(seconds: 10);

  String? _orderId;
  Timer? _trackingTimer;
  bool _trackingRefreshInFlight = false;

  Future<void> load(String orderId) async {
    if (state.isBusy) {
      return;
    }

    _orderId = orderId;
    emit(
      state.copyWith(
        status: OrderDetailStatus.loading,
        isTrackingLoading: true,
        isLiveTrackingEnabled: false,
        clearLastTrackingRefreshAt: true,
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
      _stopLiveTracking();
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to load order detail.',
      );
      emit(
        state.copyWith(
          status: OrderDetailStatus.failure,
          isTrackingLoading: false,
          errorMessage: presentation.message,
        ),
      );
    }
  }

  Future<void> refreshTracking({bool silent = false}) async {
    final orderId = _orderId;
    if (orderId == null || _trackingRefreshInFlight) {
      return;
    }

    _trackingRefreshInFlight = true;
    if (!silent) {
      emit(state.copyWith(isTrackingLoading: true));
    }
    try {
      final detail = await _repository.getOrderDetail(orderId);
      final points = await _repository.getTrackingPoints(orderId);
      emit(
        state.copyWith(
          order: detail,
          trackingPoints: points,
          isTrackingLoading: false,
          lastTrackingRefreshAt: DateTime.now(),
          clearError: true,
        ),
      );
      _syncLiveTracking(detail.status);
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
    } finally {
      _trackingRefreshInFlight = false;
    }
  }

  void toggleLiveTracking(bool enabled) {
    if (enabled) {
      final order = state.order;
      if (order == null || !_canLiveTrack(order.status)) {
        return;
      }
      _startLiveTracking();
      return;
    }

    _stopLiveTracking();
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
      _syncLiveTracking(detail.status);
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

  Future<void> submitReview({required int stars, String? comment}) async {
    final orderId = _orderId;
    if (orderId == null || state.isBusy) {
      return;
    }

    emit(
      state.copyWith(
        status: OrderDetailStatus.reviewing,
        clearError: true,
        clearInfo: true,
      ),
    );

    try {
      await _repository.createReview(
        orderId: orderId,
        stars: stars,
        comment: comment,
      );
      emit(
        state.copyWith(
          status: OrderDetailStatus.success,
          infoMessage: 'Review submitted.',
          clearError: true,
        ),
      );
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to submit review.',
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

  void _syncLiveTracking(String status) {
    if (_canLiveTrack(status)) {
      _startLiveTracking();
    } else {
      _stopLiveTracking();
    }
  }

  void _startLiveTracking() {
    _trackingTimer?.cancel();
    emit(state.copyWith(isLiveTrackingEnabled: true));
    _trackingTimer = Timer.periodic(
      _liveTrackingInterval,
      (_) => refreshTracking(silent: true),
    );
  }

  void _stopLiveTracking() {
    _trackingTimer?.cancel();
    _trackingTimer = null;
    if (!isClosed && state.isLiveTrackingEnabled) {
      emit(state.copyWith(isLiveTrackingEnabled: false));
    }
  }

  bool _canLiveTrack(String status) {
    return switch (status.toUpperCase()) {
      'ASSIGNED' || 'PREPARING' || 'DELIVERING' => true,
      _ => false,
    };
  }

  @override
  Future<void> close() {
    _trackingTimer?.cancel();
    return super.close();
  }
}
