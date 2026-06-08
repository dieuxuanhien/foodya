import 'package:bloc_test/bloc_test.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:foodya_mobile/features/customer/domain/models/create_order_request.dart';
import 'package:foodya_mobile/features/customer/presentation/cubit/checkout_cubit.dart';
import 'package:foodya_mobile/features/customer/presentation/cubit/checkout_state.dart';
import 'package:foodya_mobile/features/customer/presentation/cubit/restaurant_browse_cubit.dart';
import 'package:foodya_mobile/features/customer/presentation/cubit/restaurant_browse_state.dart';
import 'package:foodya_mobile/features/customer/presentation/cubit/restaurant_detail_cubit.dart';
import 'package:foodya_mobile/features/customer/presentation/cubit/restaurant_detail_state.dart';
import 'package:geolocator/geolocator.dart';
import 'package:mocktail/mocktail.dart';

import '../../helpers/mock_repositories.dart';
import '../../helpers/test_models.dart';

class _FakeCreateOrderRequest extends Fake implements CreateOrderRequest {}

void main() {
  setUpAll(() {
    registerFallbackValue(_FakeCreateOrderRequest());
  });

  group('CheckoutCubit', () {
    late MockCustomerCartRepository cartRepository;
    late MockCustomerOrderRepository orderRepository;
    late MockGeolocationService geolocationService;

    CheckoutCubit buildCubit() {
      return CheckoutCubit(
        cartRepository: cartRepository,
        orderRepository: orderRepository,
        geolocationService: geolocationService,
      );
    }

    setUp(() {
      cartRepository = MockCustomerCartRepository();
      orderRepository = MockCustomerOrderRepository();
      geolocationService = MockGeolocationService();
      when(
        () => geolocationService.getFriendlyAddressForCoordinates(
          lat: any(named: 'lat'),
          lng: any(named: 'lng'),
        ),
      ).thenAnswer((_) async => '1 Nguyen Hue');
    });

    blocTest<CheckoutCubit, CheckoutState>(
      'loads empty cart state',
      build: () {
        when(
          () => cartRepository.getActiveCart(),
        ).thenAnswer((_) async => activeCart(empty: true));
        return buildCubit();
      },
      act: (cubit) => cubit.loadCart(),
      expect:
          () => [
            isA<CheckoutState>().having(
              (state) => state.status,
              'status',
              CheckoutStatus.loading,
            ),
            isA<CheckoutState>().having(
              (state) => state.status,
              'status',
              CheckoutStatus.empty,
            ),
          ],
    );

    test('validates address and location before cost review', () async {
      when(
        () => cartRepository.getActiveCart(),
      ).thenAnswer((_) async => activeCart());
      final cubit = buildCubit();

      await cubit.loadCart();
      await cubit.reviewCost();

      expect(cubit.state.errorMessage, 'Delivery address is required.');

      cubit.updateDeliveryAddress('1 Nguyen Hue');
      await cubit.reviewCost();

      expect(cubit.state.errorMessage, 'Delivery location is required.');
      verifyNever(() => orderRepository.reviewOrderCost(any()));
    });

    test(
      'reviews cost and reuses idempotency key across submit retry',
      () async {
        when(
          () => cartRepository.getActiveCart(),
        ).thenAnswer((_) async => activeCart());
        when(
          () => orderRepository.reviewOrderCost(any()),
        ).thenAnswer((_) async => orderCostReview());
        var createCalls = 0;
        when(
          () => orderRepository.createOrder(
            request: any(named: 'request'),
            idempotencyKey: any(named: 'idempotencyKey'),
          ),
        ).thenAnswer((invocation) async {
          createCalls++;
          if (createCalls == 1) {
            throw Exception('temporary');
          }
          return orderCreated();
        });
        final cubit = buildCubit();

        await cubit.loadCart();
        cubit.updateDeliveryAddress('1 Nguyen Hue');
        await cubit.updateLocation(lat: 10.77, lng: 106.7);
        await cubit.reviewCost();
        await cubit.submitOrder();
        await cubit.submitOrder();

        final captured =
            verify(
              () => orderRepository.createOrder(
                request: any(named: 'request'),
                idempotencyKey: captureAny(named: 'idempotencyKey'),
              ),
            ).captured;
        expect(captured, hasLength(2));
        expect(captured.first, captured.last);
        expect(cubit.state.status, CheckoutStatus.success);
      },
    );

    test('handles current location service and permission failures', () async {
      when(
        () => geolocationService.isLocationServiceEnabled(),
      ).thenAnswer((_) async => false);
      final cubit = buildCubit();

      await cubit.useCurrentLocation();

      expect(cubit.state.errorMessage, 'Location service is turned off.');

      when(
        () => geolocationService.isLocationServiceEnabled(),
      ).thenAnswer((_) async => true);
      when(
        () => geolocationService.requestPermission(),
      ).thenAnswer((_) async => LocationPermission.denied);

      await cubit.useCurrentLocation();

      expect(cubit.state.errorMessage, 'Location permission denied.');
    });
  });

  group('RestaurantBrowseCubit', () {
    late MockCustomerCatalogRepository repository;
    late MockGeolocationService geolocationService;

    RestaurantBrowseCubit buildCubit() {
      return RestaurantBrowseCubit(
        repository: repository,
        geolocationService: geolocationService,
      );
    }

    setUp(() {
      repository = MockCustomerCatalogRepository();
      geolocationService = MockGeolocationService();
    });

    test(
      'initializes, applies filters, and pages restaurant results',
      () async {
        when(
          () => repository.listCategoryTaxonomies(),
        ).thenAnswer((_) async => [categoryTaxonomy()]);
        when(
          () => repository.searchRestaurants(
            keyword: any(named: 'keyword'),
            minRating: any(named: 'minRating'),
            openNow: any(named: 'openNow'),
            sort: any(named: 'sort'),
            taxonomyCodes: any(named: 'taxonomyCodes'),
            page: any(named: 'page'),
            size: any(named: 'size'),
          ),
        ).thenAnswer(
          (invocation) async => restaurantSearchPage(
            items: [
              restaurantSearchItem(
                id: 'restaurant-${invocation.namedArguments[#page] ?? 0}',
              ),
            ],
            page: invocation.namedArguments[#page] as int? ?? 0,
            totalPages: 2,
          ),
        );
        final cubit = buildCubit();

        await cubit.initialize();
        await cubit.applyKeyword('pho');
        await cubit.toggleTaxonomy('vietnamese');
        await cubit.nextPage();

        expect(cubit.state.status, RestaurantBrowseStatus.success);
        expect(cubit.state.selectedTaxonomyCodes, contains('vietnamese'));
        expect(cubit.state.restaurants, hasLength(1));
        expect(cubit.state.restaurants.single.restaurantId, 'restaurant-1');
        expect(cubit.state.page, 1);
        expect(cubit.state.totalPages, 2);
        verify(
          () => repository.searchRestaurants(
            keyword: 'pho',
            minRating: any(named: 'minRating'),
            openNow: any(named: 'openNow'),
            sort: any(named: 'sort'),
            taxonomyCodes: any(named: 'taxonomyCodes'),
            page: any(named: 'page'),
            size: any(named: 'size'),
          ),
        ).called(greaterThan(0));
        verify(
          () => repository.searchRestaurants(
            keyword: any(named: 'keyword'),
            minRating: any(named: 'minRating'),
            openNow: any(named: 'openNow'),
            sort: any(named: 'sort'),
            taxonomyCodes: any(named: 'taxonomyCodes'),
            page: any(named: 'page'),
            size: RestaurantBrowseCubit.restaurantPageSize,
          ),
        ).called(greaterThan(0));
      },
    );

    test(
      'handles nearby permission denied and manual nearby success',
      () async {
        when(
          () => geolocationService.requestPermission(),
        ).thenAnswer((_) async => LocationPermission.denied);
        final cubit = buildCubit();

        await cubit.toggleNearby(true);

        expect(cubit.state.errorMessage, 'Location permission denied.');

        when(
          () => repository.nearbyRestaurants(
            lat: any(named: 'lat'),
            lng: any(named: 'lng'),
            radiusKm: any(named: 'radiusKm'),
            sort: any(named: 'sort'),
            page: any(named: 'page'),
            size: any(named: 'size'),
            forceRefresh: any(named: 'forceRefresh'),
          ),
        ).thenAnswer((_) async => restaurantSearchPage());

        await cubit.useManualNearbyLocation(latitude: 10.77, longitude: 106.7);

        expect(cubit.state.isNearby, isTrue);
        expect(cubit.state.status, RestaurantBrowseStatus.success);
      },
    );
  });

  group('RestaurantDetailCubit', () {
    late MockCustomerCatalogRepository repository;

    RestaurantDetailCubit buildCubit() {
      return RestaurantDetailCubit(repository: repository);
    }

    setUp(() {
      repository = MockCustomerCatalogRepository();
    });

    test('loads detail, menu, reviews, filters, and load more', () async {
      when(
        () => repository.listCategoryTaxonomies(),
      ).thenAnswer((_) async => [categoryTaxonomy()]);
      when(
        () => repository.getRestaurantDetail(any()),
      ).thenAnswer((_) async => restaurantDetail());
      when(
        () => repository.getRestaurantMenuItems(
          any(),
          keyword: any(named: 'keyword'),
          sort: any(named: 'sort'),
          taxonomyCodes: any(named: 'taxonomyCodes'),
          page: any(named: 'page'),
          size: any(named: 'size'),
        ),
      ).thenAnswer(
        (invocation) async => menuPage(
          items: [
            restaurantMenuItem(
              id: 'item-${invocation.namedArguments[#page] ?? 0}',
            ),
          ],
          page: invocation.namedArguments[#page] as int? ?? 0,
          totalPages: 2,
        ),
      );
      when(
        () => repository.listRestaurantReviews(any()),
      ).thenAnswer((_) async => [orderReview()]);
      final cubit = buildCubit();

      await cubit.load('restaurant-1');
      await cubit.updateMenuKeyword('pho');
      await cubit.loadMoreMenu();

      expect(cubit.state.status, RestaurantDetailStatus.success);
      expect(cubit.state.restaurant?.name, 'Pho House');
      expect(cubit.state.reviews, hasLength(1));
      expect(cubit.state.menuItems, hasLength(2));
    });

    test('keeps loaded detail when review refresh fails', () async {
      when(
        () => repository.listCategoryTaxonomies(),
      ).thenAnswer((_) async => [categoryTaxonomy()]);
      when(
        () => repository.getRestaurantDetail(any()),
      ).thenAnswer((_) async => restaurantDetail());
      when(
        () => repository.getRestaurantMenuItems(
          any(),
          keyword: any(named: 'keyword'),
          sort: any(named: 'sort'),
          taxonomyCodes: any(named: 'taxonomyCodes'),
          page: any(named: 'page'),
          size: any(named: 'size'),
        ),
      ).thenAnswer((_) async => menuPage());
      when(
        () => repository.listRestaurantReviews(any()),
      ).thenThrow(Exception('reviews'));
      final cubit = buildCubit();

      await cubit.load('restaurant-1');

      expect(cubit.state.restaurant?.name, 'Pho House');
      expect(cubit.state.status, RestaurantDetailStatus.success);
      expect(cubit.state.errorMessage, isNotEmpty);
    });
  });
}
