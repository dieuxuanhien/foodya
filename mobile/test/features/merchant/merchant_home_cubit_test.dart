import 'package:flutter_test/flutter_test.dart';
import 'package:image_picker/image_picker.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_order_detail.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_order_summary.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_restaurant.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_restaurant_request.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_revenue_report.dart';
import 'package:foodya_mobile/features/merchant/domain/repositories/merchant_order_repository.dart';
import 'package:foodya_mobile/features/merchant/domain/repositories/merchant_restaurant_repository.dart';
import 'package:foodya_mobile/features/merchant/domain/repositories/merchant_revenue_repository.dart';
import 'package:foodya_mobile/features/merchant/presentation/cubit/merchant_home_cubit.dart';
import 'package:foodya_mobile/features/merchant/presentation/cubit/merchant_home_state.dart';

class _FakeMerchantRestaurantRepository
    implements MerchantRestaurantRepository {
  _FakeMerchantRestaurantRepository({this.shouldFail = false});

  final bool shouldFail;
  List<MerchantRestaurant> restaurants = const [
    MerchantRestaurant(
      id: 'restaurant-1',
      name: 'Pho House',
      cuisineType: 'Vietnamese',
      description: 'Soup',
      addressLine: '1 Nguyen Hue',
      latitude: 10.77,
      longitude: 106.7,
      avgRating: 4.6,
      reviewCount: 10,
      status: 'APPROVED',
      open: true,
      maxDeliveryKm: 5,
    ),
  ];

  @override
  Future<List<MerchantRestaurant>> listRestaurants() async {
    if (shouldFail) {
      throw Exception('network');
    }
    return restaurants;
  }

  @override
  Future<MerchantRestaurant> createRestaurant(
    MerchantRestaurantRequest request, {
    XFile? backgroundFile,
    XFile? avatarFile,
  }) {
    throw UnimplementedError();
  }

  @override
  Future<MerchantRestaurant> updateRestaurant({
    required String restaurantId,
    required MerchantRestaurantRequest request,
    XFile? backgroundFile,
    XFile? avatarFile,
  }) {
    throw UnimplementedError();
  }
}

class _FakeMerchantOrderRepository implements MerchantOrderRepository {
  List<MerchantOrderSummary> orders = const [
    MerchantOrderSummary(
      orderId: 'order-1',
      orderCode: 'FDY-001',
      customerName: 'Alice',
      restaurantName: 'Pho House',
      status: 'PENDING',
      paymentStatus: 'UNPAID',
      totalAmount: 55000,
    ),
    MerchantOrderSummary(
      orderId: 'order-2',
      orderCode: 'FDY-002',
      customerName: 'Ben',
      restaurantName: 'Pho House',
      status: 'PREPARING',
      paymentStatus: 'PAID',
      totalAmount: 70000,
    ),
    MerchantOrderSummary(
      orderId: 'order-3',
      orderCode: 'FDY-003',
      customerName: 'Chi',
      restaurantName: 'Pho House',
      status: 'SUCCESS',
      paymentStatus: 'PAID',
      totalAmount: 40000,
    ),
  ];

  @override
  Future<List<MerchantOrderSummary>> listRestaurantOrders(
    String restaurantId,
  ) async {
    return orders;
  }

  @override
  Future<MerchantOrderDetail> getOrderDetail(String orderId) {
    throw UnimplementedError();
  }

  @override
  Future<MerchantOrderDetail> updateOrderStatus({
    required String orderId,
    required String status,
  }) {
    throw UnimplementedError();
  }
}

class _FakeMerchantRevenueRepository implements MerchantRevenueRepository {
  @override
  Future<MerchantRevenueReport> getRevenueReport({
    DateTime? from,
    DateTime? to,
    int topItems = 5,
  }) async {
    return MerchantRevenueReport(
      fromDate: from ?? DateTime(2026, 1, 1),
      toDate: to ?? DateTime(2026, 1, 7),
      revenue: 1250000,
      platformProfit: 150000,
      orderCount: 24,
      avgOrderValue: 52000,
      series: const [],
      topSellingItems: const [],
    );
  }
}

void main() {
  test('MerchantHomeCubit loads dashboard data and metrics', () async {
    final cubit = MerchantHomeCubit(
      restaurantRepository: _FakeMerchantRestaurantRepository(),
      orderRepository: _FakeMerchantOrderRepository(),
      revenueRepository: _FakeMerchantRevenueRepository(),
    );

    await cubit.load();

    expect(cubit.state.status, MerchantHomeStatus.success);
    expect(cubit.state.selectedRestaurant?.name, 'Pho House');
    expect(cubit.state.orders, hasLength(3));
    expect(cubit.state.pendingOrderCount, 1);
    expect(cubit.state.activeOrderCount, 2);
    expect(cubit.state.sevenDayRevenue, 1250000);
    expect(cubit.state.averageRating, 4.6);
  });

  test('MerchantHomeCubit handles empty restaurant list', () async {
    final restaurantRepo =
        _FakeMerchantRestaurantRepository()..restaurants = const [];
    final cubit = MerchantHomeCubit(
      restaurantRepository: restaurantRepo,
      orderRepository: _FakeMerchantOrderRepository(),
      revenueRepository: _FakeMerchantRevenueRepository(),
    );

    await cubit.load();

    expect(cubit.state.status, MerchantHomeStatus.success);
    expect(cubit.state.selectedRestaurant, isNull);
    expect(cubit.state.orders, isEmpty);
  });

  test('MerchantHomeCubit exposes failure state', () async {
    final cubit = MerchantHomeCubit(
      restaurantRepository: _FakeMerchantRestaurantRepository(shouldFail: true),
      orderRepository: _FakeMerchantOrderRepository(),
      revenueRepository: _FakeMerchantRevenueRepository(),
    );

    await cubit.load();

    expect(cubit.state.status, MerchantHomeStatus.failure);
    expect(cubit.state.errorMessage, isNotEmpty);
  });
}
