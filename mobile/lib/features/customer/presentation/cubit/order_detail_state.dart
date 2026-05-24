import 'package:equatable/equatable.dart';

import '../../domain/models/order_detail.dart';
import '../../domain/models/order_tracking_point.dart';

enum OrderDetailStatus {
  initial,
  loading,
  success,
  failure,
  cancelling,
  reviewing,
}

class OrderDetailState extends Equatable {
  const OrderDetailState({
    required this.status,
    required this.order,
    required this.trackingPoints,
    required this.isTrackingLoading,
    this.errorMessage,
    this.infoMessage,
  });

  const OrderDetailState.initial()
    : this(
        status: OrderDetailStatus.initial,
        order: null,
        trackingPoints: const [],
        isTrackingLoading: false,
      );

  final OrderDetailStatus status;
  final OrderDetail? order;
  final List<OrderTrackingPoint> trackingPoints;
  final bool isTrackingLoading;
  final String? errorMessage;
  final String? infoMessage;

  bool get isBusy =>
      status == OrderDetailStatus.loading ||
      status == OrderDetailStatus.cancelling ||
      status == OrderDetailStatus.reviewing;

  OrderDetailState copyWith({
    OrderDetailStatus? status,
    OrderDetail? order,
    bool clearOrder = false,
    List<OrderTrackingPoint>? trackingPoints,
    bool? isTrackingLoading,
    String? errorMessage,
    String? infoMessage,
    bool clearError = false,
    bool clearInfo = false,
  }) {
    return OrderDetailState(
      status: status ?? this.status,
      order: clearOrder ? null : (order ?? this.order),
      trackingPoints: trackingPoints ?? this.trackingPoints,
      isTrackingLoading: isTrackingLoading ?? this.isTrackingLoading,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      infoMessage: clearInfo ? null : (infoMessage ?? this.infoMessage),
    );
  }

  @override
  List<Object?> get props => [
    status,
    order,
    trackingPoints,
    isTrackingLoading,
    errorMessage,
    infoMessage,
  ];
}
