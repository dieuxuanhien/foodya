import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:geolocator/geolocator.dart';
import 'package:foodya_mobile/core/location/geolocation_service.dart';
import 'package:foodya_mobile/features/customer/domain/models/category_taxonomy.dart';
import 'package:foodya_mobile/features/customer/domain/models/order_review.dart';
import 'package:foodya_mobile/features/customer/domain/models/paged_result.dart';
import 'package:foodya_mobile/features/customer/domain/models/restaurant_detail.dart';
import 'package:foodya_mobile/features/customer/domain/models/restaurant_menu_item.dart';
import 'package:foodya_mobile/features/customer/domain/models/restaurant_search_item.dart';
import 'package:foodya_mobile/features/customer/domain/repositories/customer_catalog_repository.dart';
import 'package:foodya_mobile/features/customer/presentation/pages/customer_home_page.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_order_detail.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_order_summary.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_restaurant.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_restaurant_request.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_revenue_report.dart';
import 'package:foodya_mobile/features/merchant/domain/repositories/merchant_order_repository.dart';
import 'package:foodya_mobile/features/merchant/domain/repositories/merchant_restaurant_repository.dart';
import 'package:foodya_mobile/features/merchant/domain/repositories/merchant_revenue_repository.dart';
import 'package:foodya_mobile/features/merchant/presentation/pages/merchant_home_page.dart';

class _FakeGeolocationService implements GeolocationService {
  @override
  Future<Position> getCurrentPosition({
    LocationAccuracy accuracy = LocationAccuracy.high,
    Duration? timeLimit,
  }) async {
    return Position(
      longitude: 106.7,
      latitude: 10.77,
      timestamp: DateTime(2026, 1, 1),
      accuracy: 1,
      altitude: 0,
      altitudeAccuracy: 0,
      heading: 0,
      headingAccuracy: 0,
      speed: 0,
      speedAccuracy: 0,
    );
  }

  @override
  Future<String?> getFriendlyAddress(Position position) async {
    return 'District 1';
  }

  @override
  Future<String?> getFriendlyAddressForCoordinates({
    required double lat,
    required double lng,
  }) async {
    return 'District 1';
  }

  @override
  Future<bool> isLocationPermissionGranted() async => true;

  @override
  Future<bool> isLocationServiceEnabled() async => true;

  @override
  Future<LocationPermission> requestPermission() async {
    return LocationPermission.whileInUse;
  }
}

class _FakeCustomerCatalogRepository implements CustomerCatalogRepository {
  @override
  Future<PagedResult<RestaurantSearchItem>> nearbyRestaurants({
    required double lat,
    required double lng,
    double radiusKm = 5.0,
    String? sort,
    int page = 0,
    int size = 10,
  }) async {
    return const PagedResult(
      items: [
        RestaurantSearchItem(
          restaurantId: 'restaurant-1',
          restaurantName: 'Pho House',
          cuisine: 'Vietnamese',
          backgroundImageUrl: null,
          avatarImageUrl: null,
          rating: 4.7,
          openStatus: true,
          maxDeliveryKm: 5,
          distanceKm: 1.2,
          matchedItems: [],
        ),
      ],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
    );
  }

  @override
  Future<RestaurantDetail> getRestaurantDetail(String restaurantId) {
    throw UnimplementedError();
  }

  @override
  Future<PagedResult<RestaurantMenuItem>> getRestaurantMenuItems(
    String restaurantId, {
    String? keyword,
    String? sort,
    List<String> taxonomyCodes = const [],
    int page = 0,
    int size = 20,
  }) {
    throw UnimplementedError();
  }

  @override
  Future<List<CategoryTaxonomy>> listCategoryTaxonomies() {
    throw UnimplementedError();
  }

  @override
  Future<List<OrderReview>> listRestaurantReviews(String restaurantId) {
    throw UnimplementedError();
  }

  @override
  Future<PagedResult<RestaurantSearchItem>> searchRestaurants({
    String? keyword,
    String? cuisine,
    double? minRating,
    bool? openNow,
    String? sort,
    List<String> taxonomyCodes = const [],
    int page = 0,
    int size = 10,
  }) {
    throw UnimplementedError();
  }
}

class _FakeMerchantRestaurantRepository
    implements MerchantRestaurantRepository {
  @override
  Future<List<MerchantRestaurant>> listRestaurants() async {
    return const [
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
  }

  @override
  Future<MerchantRestaurant> createRestaurant(
    MerchantRestaurantRequest request,
  ) {
    throw UnimplementedError();
  }

  @override
  Future<MerchantRestaurant> updateRestaurant({
    required String restaurantId,
    required MerchantRestaurantRequest request,
  }) {
    throw UnimplementedError();
  }
}

class _FakeMerchantOrderRepository implements MerchantOrderRepository {
  @override
  Future<List<MerchantOrderSummary>> listRestaurantOrders(
    String restaurantId,
  ) async {
    return const [
      MerchantOrderSummary(
        orderId: 'order-1',
        orderCode: 'FDY-001',
        customerName: 'Alice',
        restaurantName: 'Pho House',
        status: 'PENDING',
        paymentStatus: 'UNPAID',
        totalAmount: 55000,
      ),
    ];
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
      fromDate: DateTime(2026, 1, 1),
      toDate: DateTime(2026, 1, 7),
      revenue: 900000,
      platformProfit: 100000,
      orderCount: 12,
      avgOrderValue: 75000,
      series: const [],
      topSellingItems: const [],
    );
  }
}

void main() {
  testWidgets('Customer home renders ordering-first content', (tester) async {
    await tester.pumpWidget(
      MultiRepositoryProvider(
        providers: [
          RepositoryProvider<CustomerCatalogRepository>.value(
            value: _FakeCustomerCatalogRepository(),
          ),
          RepositoryProvider<GeolocationService>.value(
            value: _FakeGeolocationService(),
          ),
        ],
        child: const MaterialApp(home: CustomerHomePage()),
      ),
    );

    await tester.pumpAndSettle();

    expect(find.text('Browse food'), findsOneWidget);
    expect(find.text('District 1'), findsOneWidget);
    expect(find.text('Pho House'), findsOneWidget);

    await tester.drag(find.byType(ListView).first, const Offset(0, -420));
    await tester.pumpAndSettle();

    expect(find.text('Cart'), findsOneWidget);
    expect(find.text('Orders'), findsOneWidget);
    expect(find.text('AI picks'), findsOneWidget);
    expect(find.text('Updates'), findsOneWidget);
  });

  testWidgets('Merchant home renders operational dashboard', (tester) async {
    await tester.pumpWidget(
      MultiRepositoryProvider(
        providers: [
          RepositoryProvider<MerchantRestaurantRepository>.value(
            value: _FakeMerchantRestaurantRepository(),
          ),
          RepositoryProvider<MerchantOrderRepository>.value(
            value: _FakeMerchantOrderRepository(),
          ),
          RepositoryProvider<MerchantRevenueRepository>.value(
            value: _FakeMerchantRevenueRepository(),
          ),
        ],
        child: const MaterialApp(home: MerchantHomePage()),
      ),
    );

    await tester.pumpAndSettle();

    expect(find.text('Pho House'), findsOneWidget);
    expect(find.text('Pending'), findsOneWidget);
    expect(find.text('Active'), findsOneWidget);

    await tester.drag(find.byType(ListView).first, const Offset(0, -420));
    await tester.pumpAndSettle();

    expect(find.text('Order queue'), findsOneWidget);
    expect(find.text('FDY-001'), findsOneWidget);
  });
}
