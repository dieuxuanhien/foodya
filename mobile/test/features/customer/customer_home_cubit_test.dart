import 'package:flutter_test/flutter_test.dart';
import 'package:geolocator/geolocator.dart';
import 'package:foodya_mobile/core/location/geolocation_service.dart';
import 'package:foodya_mobile/features/customer/domain/models/category_taxonomy.dart';
import 'package:foodya_mobile/features/customer/domain/models/order_review.dart';
import 'package:foodya_mobile/features/customer/domain/models/paged_result.dart';
import 'package:foodya_mobile/features/customer/domain/models/restaurant_detail.dart';
import 'package:foodya_mobile/features/customer/domain/models/restaurant_menu_item.dart';
import 'package:foodya_mobile/features/customer/domain/models/restaurant_search_item.dart';
import 'package:foodya_mobile/features/customer/domain/repositories/customer_catalog_repository.dart';
import 'package:foodya_mobile/features/customer/presentation/cubit/customer_home_cubit.dart';
import 'package:foodya_mobile/features/customer/presentation/cubit/customer_home_state.dart';

class _FakeGeolocationService implements GeolocationService {
  int currentPositionCalls = 0;

  @override
  Future<Position> getCurrentPosition({
    LocationAccuracy accuracy = LocationAccuracy.high,
    Duration? timeLimit,
  }) async {
    currentPositionCalls++;
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
  int nearbyCalls = 0;

  @override
  Future<PagedResult<RestaurantSearchItem>> nearbyRestaurants({
    required double lat,
    required double lng,
    double radiusKm = 5.0,
    String? sort,
    int page = 0,
    int size = 10,
    bool forceRefresh = false,
  }) async {
    nearbyCalls++;
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
  Future<RestaurantDetail> getRestaurantDetail(
    String restaurantId, {
    bool forceRefresh = false,
  }) {
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

void main() {
  setUp(CustomerHomeCubit.resetSessionCacheForTesting);

  test(
    'initialize fetches once per app session and reuses cached home state',
    () async {
      final catalogRepository = _FakeCustomerCatalogRepository();
      final geolocationService = _FakeGeolocationService();

      final firstCubit = CustomerHomeCubit(
        catalogRepository: catalogRepository,
        geolocationService: geolocationService,
      );
      await firstCubit.initialize();

      expect(firstCubit.state.status, CustomerHomeStatus.success);
      expect(firstCubit.state.locationLabel, 'District 1');
      expect(
        firstCubit.state.nearbyRestaurants.single.restaurantName,
        'Pho House',
      );
      expect(geolocationService.currentPositionCalls, 1);
      expect(catalogRepository.nearbyCalls, 1);

      final secondCubit = CustomerHomeCubit(
        catalogRepository: catalogRepository,
        geolocationService: geolocationService,
      );
      await secondCubit.initialize();

      expect(secondCubit.state.status, CustomerHomeStatus.success);
      expect(secondCubit.state.locationLabel, 'District 1');
      expect(
        secondCubit.state.nearbyRestaurants.single.restaurantName,
        'Pho House',
      );
      expect(geolocationService.currentPositionCalls, 1);
      expect(catalogRepository.nearbyCalls, 1);

      await secondCubit.refreshLocation();

      expect(geolocationService.currentPositionCalls, 2);
      expect(catalogRepository.nearbyCalls, 2);
    },
  );
}
