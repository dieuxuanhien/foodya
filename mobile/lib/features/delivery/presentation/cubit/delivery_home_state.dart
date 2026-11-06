import 'package:equatable/equatable.dart';

import '../../domain/models/delivery_models.dart';

enum DeliveryHomeStatus { initial, loading, ready, actionPending, failure }

class DeliveryHomeState extends Equatable {
  const DeliveryHomeState({
    this.status = DeliveryHomeStatus.initial,
    this.session,
    this.errorMessage,
    this.infoMessage,
  });

  final DeliveryHomeStatus status;
  final DeliverySession? session;
  final String? errorMessage;
  final String? infoMessage;

  bool get isBusy =>
      status == DeliveryHomeStatus.loading ||
      status == DeliveryHomeStatus.actionPending;

  DeliveryHomeState copyWith({
    DeliveryHomeStatus? status,
    DeliverySession? session,
    String? errorMessage,
    String? infoMessage,
    bool clearError = false,
    bool clearInfo = false,
  }) => DeliveryHomeState(
    status: status ?? this.status,
    session: session ?? this.session,
    errorMessage: clearError ? null : errorMessage ?? this.errorMessage,
    infoMessage: clearInfo ? null : infoMessage ?? this.infoMessage,
  );

  @override
  List<Object?> get props => [status, session, errorMessage, infoMessage];
}
