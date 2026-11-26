import 'package:equatable/equatable.dart';

import '../../domain/models/delivery_models.dart';

enum DeliveryOrderViewStatus { initial, loading, ready, actionPending, failure }

class DeliveryOrderState extends Equatable {
  const DeliveryOrderState({
    this.status = DeliveryOrderViewStatus.initial,
    this.order,
    this.route,
    this.routeUnavailable = false,
    this.errorMessage,
    this.infoMessage,
  });

  final DeliveryOrderViewStatus status;
  final DeliveryAssignment? order;
  final DeliveryRouteSummary? route;
  final bool routeUnavailable;
  final String? errorMessage;
  final String? infoMessage;

  bool get isBusy =>
      status == DeliveryOrderViewStatus.loading ||
      status == DeliveryOrderViewStatus.actionPending;

  DeliveryOrderState copyWith({
    DeliveryOrderViewStatus? status,
    DeliveryAssignment? order,
    DeliveryRouteSummary? route,
    bool? routeUnavailable,
    String? errorMessage,
    String? infoMessage,
    bool clearError = false,
    bool clearInfo = false,
  }) => DeliveryOrderState(
    status: status ?? this.status,
    order: order ?? this.order,
    route: route ?? this.route,
    routeUnavailable: routeUnavailable ?? this.routeUnavailable,
    errorMessage: clearError ? null : errorMessage ?? this.errorMessage,
    infoMessage: clearInfo ? null : infoMessage ?? this.infoMessage,
  );

  @override
  List<Object?> get props => [
    status,
    order,
    route,
    routeUnavailable,
    errorMessage,
    infoMessage,
  ];
}
