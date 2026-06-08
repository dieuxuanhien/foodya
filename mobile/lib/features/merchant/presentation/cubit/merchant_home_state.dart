import 'package:equatable/equatable.dart';

import '../../domain/models/merchant_order_summary.dart';
import '../../domain/models/merchant_restaurant.dart';
import '../../domain/models/merchant_revenue_report.dart';

enum MerchantHomeStatus { initial, loading, success, failure }

class MerchantHomeState extends Equatable {
  const MerchantHomeState({
    required this.status,
    required this.restaurants,
    required this.orders,
    this.selectedRestaurant,
    this.revenueReport,
    this.errorMessage,
  });

  const MerchantHomeState.initial()
    : this(
        status: MerchantHomeStatus.initial,
        restaurants: const [],
        orders: const [],
      );

  final MerchantHomeStatus status;
  final List<MerchantRestaurant> restaurants;
  final MerchantRestaurant? selectedRestaurant;
  final List<MerchantOrderSummary> orders;
  final MerchantRevenueReport? revenueReport;
  final String? errorMessage;

  bool get isLoading => status == MerchantHomeStatus.loading;

  int get pendingOrderCount =>
      orders.where((order) => order.status.toUpperCase() == 'PENDING').length;

  int get activeOrderCount {
    const inactive = {'SUCCESS', 'CANCELLED', 'FAILED'};
    return orders
        .where((order) => !inactive.contains(order.status.toUpperCase()))
        .length;
  }

  double get sevenDayRevenue => revenueReport?.revenue ?? 0;

  double get averageRating => selectedRestaurant?.avgRating ?? 0;

  MerchantHomeState copyWith({
    MerchantHomeStatus? status,
    List<MerchantRestaurant>? restaurants,
    MerchantRestaurant? selectedRestaurant,
    List<MerchantOrderSummary>? orders,
    MerchantRevenueReport? revenueReport,
    String? errorMessage,
    bool clearSelectedRestaurant = false,
    bool clearRevenueReport = false,
    bool clearError = false,
  }) {
    return MerchantHomeState(
      status: status ?? this.status,
      restaurants: restaurants ?? this.restaurants,
      selectedRestaurant:
          clearSelectedRestaurant
              ? null
              : (selectedRestaurant ?? this.selectedRestaurant),
      orders: orders ?? this.orders,
      revenueReport:
          clearRevenueReport ? null : (revenueReport ?? this.revenueReport),
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
    );
  }

  @override
  List<Object?> get props => [
    status,
    restaurants,
    selectedRestaurant,
    orders,
    revenueReport,
    errorMessage,
  ];
}
