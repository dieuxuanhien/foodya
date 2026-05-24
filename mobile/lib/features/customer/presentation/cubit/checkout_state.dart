import 'package:equatable/equatable.dart';

import '../../domain/models/active_cart.dart';
import '../../domain/models/order_cost_review.dart';
import '../../domain/models/order_created.dart';

enum CheckoutStatus {
  initial,
  loading,
  ready,
  empty,
  reviewing,
  submitting,
  success,
  failure,
}

class CheckoutState extends Equatable {
  const CheckoutState({
    required this.status,
    required this.cart,
    required this.deliveryAddress,
    required this.deliveryLatitude,
    required this.deliveryLongitude,
    required this.customerNote,
    required this.costReview,
    required this.orderCreated,
    this.errorMessage,
    this.infoMessage,
  });

  const CheckoutState.initial()
    : this(
        status: CheckoutStatus.initial,
        cart: null,
        deliveryAddress: '',
        deliveryLatitude: null,
        deliveryLongitude: null,
        customerNote: '',
        costReview: null,
        orderCreated: null,
      );

  final CheckoutStatus status;
  final ActiveCart? cart;
  final String deliveryAddress;
  final double? deliveryLatitude;
  final double? deliveryLongitude;
  final String customerNote;
  final OrderCostReview? costReview;
  final OrderCreated? orderCreated;
  final String? errorMessage;
  final String? infoMessage;

  bool get isBusy =>
      status == CheckoutStatus.loading ||
      status == CheckoutStatus.reviewing ||
      status == CheckoutStatus.submitting;

  bool get hasLocation => deliveryLatitude != null && deliveryLongitude != null;

  CheckoutState copyWith({
    CheckoutStatus? status,
    ActiveCart? cart,
    bool clearCart = false,
    String? deliveryAddress,
    double? deliveryLatitude,
    double? deliveryLongitude,
    bool clearLocation = false,
    String? customerNote,
    OrderCostReview? costReview,
    bool clearCostReview = false,
    OrderCreated? orderCreated,
    bool clearOrderCreated = false,
    String? errorMessage,
    String? infoMessage,
    bool clearError = false,
    bool clearInfo = false,
  }) {
    return CheckoutState(
      status: status ?? this.status,
      cart: clearCart ? null : (cart ?? this.cart),
      deliveryAddress: deliveryAddress ?? this.deliveryAddress,
      deliveryLatitude:
          clearLocation ? null : (deliveryLatitude ?? this.deliveryLatitude),
      deliveryLongitude:
          clearLocation ? null : (deliveryLongitude ?? this.deliveryLongitude),
      customerNote: customerNote ?? this.customerNote,
      costReview: clearCostReview ? null : (costReview ?? this.costReview),
      orderCreated:
          clearOrderCreated ? null : (orderCreated ?? this.orderCreated),
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      infoMessage: clearInfo ? null : (infoMessage ?? this.infoMessage),
    );
  }

  @override
  List<Object?> get props => [
    status,
    cart,
    deliveryAddress,
    deliveryLatitude,
    deliveryLongitude,
    customerNote,
    costReview,
    orderCreated,
    errorMessage,
    infoMessage,
  ];
}
