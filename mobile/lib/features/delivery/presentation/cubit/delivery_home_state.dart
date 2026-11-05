import 'package:equatable/equatable.dart';

import '../../domain/models/delivery_models.dart';

enum DeliveryHomeStatus {
  initial,
  loading,
  ready,
  empty,
  actionPending,
  failure,
}

class DeliveryHomeState extends Equatable {
  const DeliveryHomeState({
    this.status = DeliveryHomeStatus.initial,
    this.session,
    this.assignments = const [],
    this.activeBatch,
    this.errorMessage,
    this.infoMessage,
  });

  final DeliveryHomeStatus status;
  final DeliverySession? session;
  final List<DeliveryAssignment> assignments;
  final DeliveryBatch? activeBatch;
  final String? errorMessage;
  final String? infoMessage;

  bool get isBusy =>
      status == DeliveryHomeStatus.loading ||
      status == DeliveryHomeStatus.actionPending;

  DeliveryHomeState copyWith({
    DeliveryHomeStatus? status,
    DeliverySession? session,
    List<DeliveryAssignment>? assignments,
    DeliveryBatch? activeBatch,
    bool clearBatch = false,
    String? errorMessage,
    String? infoMessage,
    bool clearError = false,
    bool clearInfo = false,
  }) {
    return DeliveryHomeState(
      status: status ?? this.status,
      session: session ?? this.session,
      assignments: assignments ?? this.assignments,
      activeBatch: clearBatch ? null : activeBatch ?? this.activeBatch,
      errorMessage: clearError ? null : errorMessage ?? this.errorMessage,
      infoMessage: clearInfo ? null : infoMessage ?? this.infoMessage,
    );
  }

  @override
  List<Object?> get props => [
    status,
    session,
    assignments,
    activeBatch,
    errorMessage,
    infoMessage,
  ];
}
