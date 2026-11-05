import 'package:equatable/equatable.dart';

import '../../domain/models/delivery_models.dart';

enum DeliveryBatchViewStatus {
  initial,
  loading,
  ready,
  empty,
  actionPending,
  failure,
}

class DeliveryBatchState extends Equatable {
  const DeliveryBatchState({
    this.status = DeliveryBatchViewStatus.initial,
    this.batch,
    this.errorMessage,
    this.infoMessage,
  });
  final DeliveryBatchViewStatus status;
  final DeliveryBatch? batch;
  final String? errorMessage;
  final String? infoMessage;
  bool get isBusy =>
      status == DeliveryBatchViewStatus.loading ||
      status == DeliveryBatchViewStatus.actionPending;

  DeliveryBatchState copyWith({
    DeliveryBatchViewStatus? status,
    DeliveryBatch? batch,
    String? errorMessage,
    String? infoMessage,
    bool clearError = false,
    bool clearInfo = false,
  }) => DeliveryBatchState(
    status: status ?? this.status,
    batch: batch ?? this.batch,
    errorMessage: clearError ? null : errorMessage ?? this.errorMessage,
    infoMessage: clearInfo ? null : infoMessage ?? this.infoMessage,
  );
  @override
  List<Object?> get props => [status, batch, errorMessage, infoMessage];
}
