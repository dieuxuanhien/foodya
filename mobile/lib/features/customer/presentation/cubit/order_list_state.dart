import 'package:equatable/equatable.dart';

import '../../domain/models/order_summary.dart';

enum OrderListStatus { initial, loading, success, empty, failure }

class OrderListState extends Equatable {
  const OrderListState({
    required this.status,
    required this.orders,
    this.errorMessage,
  });

  const OrderListState.initial()
    : this(status: OrderListStatus.initial, orders: const []);

  final OrderListStatus status;
  final List<OrderSummary> orders;
  final String? errorMessage;

  bool get isBusy => status == OrderListStatus.loading;

  OrderListState copyWith({
    OrderListStatus? status,
    List<OrderSummary>? orders,
    String? errorMessage,
    bool clearError = false,
  }) {
    return OrderListState(
      status: status ?? this.status,
      orders: orders ?? this.orders,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
    );
  }

  @override
  List<Object?> get props => [status, orders, errorMessage];
}
