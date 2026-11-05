import 'package:flutter_bloc/flutter_bloc.dart';

import '../../domain/models/delivery_models.dart';
import '../../domain/repositories/delivery_repository.dart';
import 'delivery_batch_state.dart';

class DeliveryBatchCubit extends Cubit<DeliveryBatchState> {
  DeliveryBatchCubit(this._repository) : super(const DeliveryBatchState());
  final DeliveryRepository _repository;

  Future<void> load(String batchId) async {
    if (state.isBusy) return;
    emit(
      state.copyWith(
        status: DeliveryBatchViewStatus.loading,
        clearError: true,
        clearInfo: true,
      ),
    );
    try {
      final batch =
          batchId == 'active'
              ? await _repository.getActiveBatch()
              : await _repository.getBatch(batchId);
      if (isClosed) return;
      emit(
        state.copyWith(
          status:
              batch == null
                  ? DeliveryBatchViewStatus.empty
                  : DeliveryBatchViewStatus.ready,
          batch: batch,
          clearError: true,
        ),
      );
    } catch (error) {
      if (isClosed) return;
      emit(
        state.copyWith(
          status: DeliveryBatchViewStatus.failure,
          errorMessage: error.toString(),
        ),
      );
    }
  }

  Future<void> markPickup(String orderId) =>
      _update(orderId, DeliveryStopStatus.pickedUp);
  Future<void> markDelivered(String orderId) =>
      _update(orderId, DeliveryStopStatus.delivered);
  Future<void> markFailed(String orderId) =>
      _update(orderId, DeliveryStopStatus.failed);

  Future<void> _update(String orderId, DeliveryStopStatus target) async {
    final batch = state.batch;
    if (batch == null || state.isBusy) return;
    emit(
      state.copyWith(
        status: DeliveryBatchViewStatus.actionPending,
        clearError: true,
        clearInfo: true,
      ),
    );
    try {
      final updated = switch (target) {
        DeliveryStopStatus.pickedUp => await _repository.markPickup(
          batch.batchId,
          orderId,
        ),
        DeliveryStopStatus.delivered => await _repository.markDelivered(
          batch.batchId,
          orderId,
        ),
        DeliveryStopStatus.failed => await _repository.markFailed(
          batch.batchId,
          orderId,
        ),
        _ => batch,
      };
      if (isClosed) return;
      emit(
        state.copyWith(
          status: DeliveryBatchViewStatus.ready,
          batch: updated,
          infoMessage: 'Demo batch progress updated.',
        ),
      );
    } catch (error) {
      if (isClosed) return;
      emit(
        state.copyWith(
          status: DeliveryBatchViewStatus.failure,
          errorMessage: error.toString(),
        ),
      );
    }
  }

  void clearFeedback() {
    if (!isClosed) emit(state.copyWith(clearError: true, clearInfo: true));
  }
}
