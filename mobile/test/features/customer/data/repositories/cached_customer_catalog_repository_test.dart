import 'package:flutter_test/flutter_test.dart';
import 'package:foodya_mobile/features/customer/data/repositories/cached_customer_catalog_repository.dart';
import 'package:foodya_mobile/features/customer/domain/models/order_review.dart';
import 'package:foodya_mobile/features/customer/domain/models/paged_result.dart';
import 'package:foodya_mobile/features/customer/domain/models/restaurant_menu_item.dart';
import 'package:foodya_mobile/features/customer/domain/models/restaurant_search_item.dart';
import 'package:mocktail/mocktail.dart';

import '../../../../helpers/mock_repositories.dart';
import '../../../../helpers/test_models.dart';

void main() {
  late MockCustomerCatalogRepository delegate;
  late CachedCustomerCatalogRepository cached;

  setUpAll(() {
    registerFallbackValue(restaurantDetail());
  });

  setUp(() {
    delegate = MockCustomerCatalogRepository();
    cached = CachedCustomerCatalogRepository(delegate: delegate);
  });

  PagedResult<RestaurantSearchItem> nearbyResult() => const PagedResult(
    items: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
  );

  group('listCategoryTaxonomies', () {
    test('caches the result and only calls the delegate once', () async {
      when(
        () => delegate.listCategoryTaxonomies(),
      ).thenAnswer((_) async => [categoryTaxonomy()]);

      final first = await cached.listCategoryTaxonomies();
      final second = await cached.listCategoryTaxonomies();

      expect(first, second);
      verify(() => delegate.listCategoryTaxonomies()).called(1);
    });
  });

  group('getRestaurantDetail', () {
    test('caches per restaurant id', () async {
      when(
        () => delegate.getRestaurantDetail(any()),
      ).thenAnswer((_) async => restaurantDetail());

      await cached.getRestaurantDetail('restaurant-1');
      await cached.getRestaurantDetail('restaurant-1');
      await cached.getRestaurantDetail('restaurant-2');

      verify(() => delegate.getRestaurantDetail('restaurant-1')).called(1);
      verify(() => delegate.getRestaurantDetail('restaurant-2')).called(1);
    });

    test('forceRefresh bypasses and refreshes the cached entry', () async {
      when(
        () => delegate.getRestaurantDetail(any()),
      ).thenAnswer((_) async => restaurantDetail());

      await cached.getRestaurantDetail('restaurant-1');
      await cached.getRestaurantDetail('restaurant-1', forceRefresh: true);

      verify(() => delegate.getRestaurantDetail('restaurant-1')).called(2);
    });
  });

  group('nearbyRestaurants', () {
    test('caches by location key and only calls the delegate once', () async {
      when(
        () => delegate.nearbyRestaurants(
          lat: any(named: 'lat'),
          lng: any(named: 'lng'),
          radiusKm: any(named: 'radiusKm'),
          sort: any(named: 'sort'),
          page: any(named: 'page'),
          size: any(named: 'size'),
        ),
      ).thenAnswer((_) async => nearbyResult());

      await cached.nearbyRestaurants(lat: 10.77, lng: 106.7);
      await cached.nearbyRestaurants(lat: 10.77, lng: 106.7);

      verify(
        () => delegate.nearbyRestaurants(
          lat: any(named: 'lat'),
          lng: any(named: 'lng'),
          radiusKm: any(named: 'radiusKm'),
          sort: any(named: 'sort'),
          page: any(named: 'page'),
          size: any(named: 'size'),
        ),
      ).called(1);
    });

    test('forceRefresh bypasses the cache', () async {
      when(
        () => delegate.nearbyRestaurants(
          lat: any(named: 'lat'),
          lng: any(named: 'lng'),
          radiusKm: any(named: 'radiusKm'),
          sort: any(named: 'sort'),
          page: any(named: 'page'),
          size: any(named: 'size'),
        ),
      ).thenAnswer((_) async => nearbyResult());

      await cached.nearbyRestaurants(lat: 10.77, lng: 106.7);
      await cached.nearbyRestaurants(lat: 10.77, lng: 106.7, forceRefresh: true);

      verify(
        () => delegate.nearbyRestaurants(
          lat: any(named: 'lat'),
          lng: any(named: 'lng'),
          radiusKm: any(named: 'radiusKm'),
          sort: any(named: 'sort'),
          page: any(named: 'page'),
          size: any(named: 'size'),
        ),
      ).called(2);
    });
  });

  group('uncached pass-through methods', () {
    test('searchRestaurants always calls the delegate', () async {
      when(
        () => delegate.searchRestaurants(
          keyword: any(named: 'keyword'),
          cuisine: any(named: 'cuisine'),
          minRating: any(named: 'minRating'),
          openNow: any(named: 'openNow'),
          sort: any(named: 'sort'),
          taxonomyCodes: any(named: 'taxonomyCodes'),
          page: any(named: 'page'),
          size: any(named: 'size'),
        ),
      ).thenAnswer((_) async => nearbyResult());

      await cached.searchRestaurants(keyword: 'pho');
      await cached.searchRestaurants(keyword: 'pho');

      verify(
        () => delegate.searchRestaurants(
          keyword: any(named: 'keyword'),
          cuisine: any(named: 'cuisine'),
          minRating: any(named: 'minRating'),
          openNow: any(named: 'openNow'),
          sort: any(named: 'sort'),
          taxonomyCodes: any(named: 'taxonomyCodes'),
          page: any(named: 'page'),
          size: any(named: 'size'),
        ),
      ).called(2);
    });

    test('getRestaurantMenuItems and listRestaurantReviews pass through', () async {
      when(
        () => delegate.getRestaurantMenuItems(
          any(),
          keyword: any(named: 'keyword'),
          sort: any(named: 'sort'),
          taxonomyCodes: any(named: 'taxonomyCodes'),
          page: any(named: 'page'),
          size: any(named: 'size'),
        ),
      ).thenAnswer(
        (_) async => const PagedResult<RestaurantMenuItem>(
          items: [],
          page: 0,
          size: 20,
          totalElements: 0,
          totalPages: 0,
        ),
      );
      when(
        () => delegate.listRestaurantReviews(any()),
      ).thenAnswer((_) async => const <OrderReview>[]);

      await cached.getRestaurantMenuItems('restaurant-1');
      await cached.listRestaurantReviews('restaurant-1');

      verify(
        () => delegate.getRestaurantMenuItems(
          'restaurant-1',
          keyword: any(named: 'keyword'),
          sort: any(named: 'sort'),
          taxonomyCodes: any(named: 'taxonomyCodes'),
          page: any(named: 'page'),
          size: any(named: 'size'),
        ),
      ).called(1);
      verify(() => delegate.listRestaurantReviews('restaurant-1')).called(1);
    });
  });
}
