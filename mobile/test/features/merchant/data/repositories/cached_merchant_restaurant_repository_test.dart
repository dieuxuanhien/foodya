import 'package:flutter_test/flutter_test.dart';
import 'package:foodya_mobile/features/merchant/data/repositories/cached_merchant_restaurant_repository.dart';
import 'package:mocktail/mocktail.dart';

import '../../../../helpers/mock_repositories.dart';
import '../../../../helpers/test_models.dart';

void main() {
  late MockMerchantRestaurantRepository delegate;
  late CachedMerchantRestaurantRepository cached;

  setUpAll(() {
    registerFallbackValue(merchantRestaurantRequest());
  });

  setUp(() {
    delegate = MockMerchantRestaurantRepository();
    cached = CachedMerchantRestaurantRepository(delegate: delegate);
  });

  group('listRestaurants', () {
    test('caches the result and only calls the delegate once', () async {
      when(
        () => delegate.listRestaurants(),
      ).thenAnswer((_) async => [merchantRestaurant()]);

      final first = await cached.listRestaurants();
      final second = await cached.listRestaurants();

      expect(first, second);
      verify(() => delegate.listRestaurants()).called(1);
    });

    test('forceRefresh bypasses and refreshes the cached entry', () async {
      when(
        () => delegate.listRestaurants(),
      ).thenAnswer((_) async => [merchantRestaurant()]);

      await cached.listRestaurants();
      await cached.listRestaurants(forceRefresh: true);

      verify(() => delegate.listRestaurants()).called(2);
    });

    test('clearCache forces the next call back to the delegate', () async {
      when(
        () => delegate.listRestaurants(),
      ).thenAnswer((_) async => [merchantRestaurant()]);

      await cached.listRestaurants();
      cached.clearCache();
      await cached.listRestaurants();

      verify(() => delegate.listRestaurants()).called(2);
    });
  });

  group('mutating calls', () {
    test('createRestaurant invalidates the cached list', () async {
      when(
        () => delegate.listRestaurants(),
      ).thenAnswer((_) async => [merchantRestaurant()]);
      when(
        () => delegate.createRestaurant(
          any(),
          backgroundFile: any(named: 'backgroundFile'),
          avatarFile: any(named: 'avatarFile'),
        ),
      ).thenAnswer((_) async => merchantRestaurant());

      await cached.listRestaurants();
      await cached.createRestaurant(merchantRestaurantRequest());
      await cached.listRestaurants();

      verify(() => delegate.listRestaurants()).called(2);
    });

    test('updateRestaurant invalidates the cached list', () async {
      when(
        () => delegate.listRestaurants(),
      ).thenAnswer((_) async => [merchantRestaurant()]);
      when(
        () => delegate.updateRestaurant(
          restaurantId: any(named: 'restaurantId'),
          request: any(named: 'request'),
          backgroundFile: any(named: 'backgroundFile'),
          avatarFile: any(named: 'avatarFile'),
        ),
      ).thenAnswer((_) async => merchantRestaurant());

      await cached.listRestaurants();
      await cached.updateRestaurant(
        restaurantId: 'restaurant-1',
        request: merchantRestaurantRequest(),
      );
      await cached.listRestaurants();

      verify(() => delegate.listRestaurants()).called(2);
    });
  });
}
