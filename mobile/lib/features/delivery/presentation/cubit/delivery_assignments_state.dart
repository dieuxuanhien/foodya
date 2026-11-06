import 'package:equatable/equatable.dart';

import '../../domain/models/delivery_models.dart';

enum DeliveryAssignmentsStatus { initial, loading, ready, empty, failure }

class DeliveryAssignmentsState extends Equatable {
  const DeliveryAssignmentsState({
    this.status = DeliveryAssignmentsStatus.initial,
    this.assignments = const [],
    this.pendingOrderId,
    this.errorMessage,
    this.infoMessage,
  });

  final DeliveryAssignmentsStatus status;
  final List<DeliveryAssignment> assignments;
  final String? pendingOrderId;
  final String? errorMessage;
  final String? infoMessage;

  DeliveryAssignmentsState copyWith({
    DeliveryAssignmentsStatus? status,
    List<DeliveryAssignment>? assignments,
    String? pendingOrderId,
    bool clearPending = false,
    String? errorMessage,
    String? infoMessage,
    bool clearError = false,
    bool clearInfo = false,
  }) => DeliveryAssignmentsState(
    status: status ?? this.status,
    assignments: assignments ?? this.assignments,
    pendingOrderId: clearPending ? null : pendingOrderId ?? this.pendingOrderId,
    errorMessage: clearError ? null : errorMessage ?? this.errorMessage,
    infoMessage: clearInfo ? null : infoMessage ?? this.infoMessage,
  );

  @override
  List<Object?> get props => [
    status,
    assignments,
    pendingOrderId,
    errorMessage,
    infoMessage,
  ];
}
