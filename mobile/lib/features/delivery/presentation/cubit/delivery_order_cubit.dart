import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/network/api_error_ui_message.dart';
import '../../domain/models/delivery_models.dart';
import '../../domain/repositories/delivery_repository.dart';
import 'delivery_order_state.dart';

class DeliveryOrderCubit extends Cubit<DeliveryOrderState> {
  DeliveryOrderCubit(this._repository) : super(const DeliveryOrderState());
  final DeliveryRepository _repository;

  Future<void> load(String orderId, {DeliveryAssignment? seed}) async {
    if (state.isBusy) return;
    emit(
      state.copyWith(
        status: DeliveryOrderViewStatus.loading,
        order: seed,
        clearError: true,
        clearInfo: true,
      ),
    );
    try {
      var order = seed;
      order ??=
          (await _repository.getAssignments())
              .where((item) => item.orderId == orderId)
              .firstOrNull;
      if (order == null) {
        throw StateError(
          'This order is no longer in the driver assignment list.',
        );
      }
      DeliveryRouteSummary? route;
      var routeUnavailable = false;
      try {
        route = await _repository.getRoute(orderId);
      } catch (_) {
        routeUnavailable = true;
      }
      if (isClosed) return;
      emit(
        state.copyWith(
          status: DeliveryOrderViewStatus.ready,
          order: order,
          route: route,
          routeUnavailable: routeUnavailable,
        ),
      );
    } catch (error) {
      if (isClosed) return;
      final message =
          ApiErrorUiMessageMapper.mapAny(
            error,
            fallback: 'Unable to load this assigned order.',
          ).message;
      emit(
        state.copyWith(
          status: DeliveryOrderViewStatus.failure,
          errorMessage: message,
        ),
      );
    }
  }

  Future<void> acknowledge() async {
    final order = state.order;
    if (order == null || state.isBusy) return;
    await _action(
      () => _repository.acceptAssignment(order.orderId),
      'Assignment acknowledged. This is not a durable acceptance flag.',
    );
  }

  Future<void> updateStatus(DeliveryOrderStatus status) async {
    final order = state.order;
    if (order == null || state.isBusy) return;
    await _action(
      () => _repository.updateOrderStatus(order.orderId, status),
      switch (status) {
        DeliveryOrderStatus.delivering => 'Delivery started.',
        DeliveryOrderStatus.success => 'Delivery completed successfully.',
        DeliveryOrderStatus.failed => 'Delivery marked as failed.',
        _ => 'Order status updated.',
      },
    );
  }

  Future<void> _action(
    Future<DeliveryAssignment> Function() action,
    String message,
  ) async {
    emit(
      state.copyWith(
        status: DeliveryOrderViewStatus.actionPending,
        clearError: true,
        clearInfo: true,
      ),
    );
    try {
      final updated = await action();
      if (isClosed) return;
      emit(
        state.copyWith(
          status: DeliveryOrderViewStatus.ready,
          order: updated,
          infoMessage: message,
        ),
      );
    } catch (error) {
      if (isClosed) return;
      final mapped = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to update this order.',
      );
      emit(
        state.copyWith(
          status: DeliveryOrderViewStatus.ready,
          errorMessage: mapped.message,
        ),
      );
    }
  }

  void clearFeedback() {
    if (!isClosed) emit(state.copyWith(clearError: true, clearInfo: true));
  }
}

extension _FirstOrNull<T> on Iterable<T> {
  T? get firstOrNull {
    final iterator = this.iterator;
    return iterator.moveNext() ? iterator.current : null;
  }
}
