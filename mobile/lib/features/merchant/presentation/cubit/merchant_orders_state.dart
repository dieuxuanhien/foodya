import 'package:equatable/equatable.dart';

import '../../domain/models/merchant_order_detail.dart';
import '../../domain/models/merchant_order_summary.dart';
import '../../domain/models/merchant_restaurant.dart';

enum MerchantOrdersStatus { initial, loading, acting, success, failure }

const Object _unset = Object();

class MerchantOrdersState extends Equatable {
  const MerchantOrdersState({
    required this.status,
    this.restaurants = const [],
    this.orders = const [],
    this.selectedRestaurant,
    this.selectedOrder,
    this.errorMessage,
    this.infoMessage,
  });

  const MerchantOrdersState.initial()
    : this(status: MerchantOrdersStatus.initial);

  final MerchantOrdersStatus status;
  final List<MerchantRestaurant> restaurants;
  final List<MerchantOrderSummary> orders;
  final MerchantRestaurant? selectedRestaurant;
  final MerchantOrderDetail? selectedOrder;
  final String? errorMessage;
  final String? infoMessage;

  bool get isLoading => status == MerchantOrdersStatus.loading;
  bool get isActing => status == MerchantOrdersStatus.acting;
  bool get isBusy => isLoading || isActing;

  MerchantOrdersState copyWith({
    MerchantOrdersStatus? status,
    List<MerchantRestaurant>? restaurants,
    List<MerchantOrderSummary>? orders,
    Object? selectedRestaurant = _unset,
    Object? selectedOrder = _unset,
    String? errorMessage,
    String? infoMessage,
    bool clearError = false,
    bool clearInfo = false,
  }) {
    return MerchantOrdersState(
      status: status ?? this.status,
      restaurants: restaurants ?? this.restaurants,
      orders: orders ?? this.orders,
      selectedRestaurant:
          selectedRestaurant == _unset
              ? this.selectedRestaurant
              : selectedRestaurant as MerchantRestaurant?,
      selectedOrder:
          selectedOrder == _unset
              ? this.selectedOrder
              : selectedOrder as MerchantOrderDetail?,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      infoMessage: clearInfo ? null : (infoMessage ?? this.infoMessage),
    );
  }

  @override
  List<Object?> get props => [
    status,
    restaurants,
    orders,
    selectedRestaurant,
    selectedOrder,
    errorMessage,
    infoMessage,
  ];
}
