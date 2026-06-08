import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:foodya_mobile/app.dart';
import 'package:foodya_mobile/core/auth/session_cubit.dart';
import 'package:foodya_mobile/core/auth/user_role.dart';
import 'package:foodya_mobile/core/location/geolocation_service.dart';
import 'package:foodya_mobile/core/router/app_router.dart';
import 'package:foodya_mobile/features/auth/domain/repositories/auth_repository.dart';
import 'package:foodya_mobile/features/auth/presentation/cubit/login_cubit.dart';
import 'package:foodya_mobile/features/customer/domain/repositories/customer_ai_repository.dart';
import 'package:foodya_mobile/features/customer/domain/repositories/customer_cart_repository.dart';
import 'package:foodya_mobile/features/customer/domain/repositories/customer_catalog_repository.dart';
import 'package:foodya_mobile/features/customer/domain/repositories/customer_notification_repository.dart';
import 'package:foodya_mobile/features/customer/domain/repositories/customer_order_repository.dart';
import 'package:foodya_mobile/features/customer/domain/repositories/customer_profile_repository.dart';
import 'package:foodya_mobile/features/merchant/domain/repositories/merchant_catalog_repository.dart';
import 'package:foodya_mobile/features/merchant/domain/repositories/merchant_notification_repository.dart';
import 'package:foodya_mobile/features/merchant/domain/repositories/merchant_order_repository.dart';
import 'package:foodya_mobile/features/merchant/domain/repositories/merchant_restaurant_repository.dart';
import 'package:foodya_mobile/features/merchant/domain/repositories/merchant_revenue_repository.dart';
import 'package:foodya_mobile/features/merchant/domain/repositories/merchant_review_repository.dart';
import 'package:geolocator/geolocator.dart';
import 'package:go_router/go_router.dart';
import 'package:mocktail/mocktail.dart';

import '../../helpers/mock_repositories.dart';
import '../../helpers/test_models.dart';

void main() {
  late SessionCubit sessionCubit;
  late MockAuthRepository authRepository;
  late MockGeolocationService geolocationService;
  late MockCustomerCatalogRepository customerCatalogRepository;
  late MockCustomerCartRepository customerCartRepository;
  late MockCustomerOrderRepository customerOrderRepository;
  late MockCustomerProfileRepository customerProfileRepository;
  late MockCustomerNotificationRepository customerNotificationRepository;
  late MockCustomerAiRepository customerAiRepository;
  late MockMerchantRestaurantRepository merchantRestaurantRepository;
  late MockMerchantCatalogRepository merchantCatalogRepository;
  late MockMerchantOrderRepository merchantOrderRepository;
  late MockMerchantReviewRepository merchantReviewRepository;
  late MockMerchantRevenueRepository merchantRevenueRepository;
  late MockMerchantNotificationRepository merchantNotificationRepository;

  setUpAll(() {
    registerFallbackValue(LocationAccuracy.high);
    registerFallbackValue(
      Position(
        longitude: 0,
        latitude: 0,
        timestamp: DateTime(2026, 1, 1),
        accuracy: 0,
        altitude: 0,
        altitudeAccuracy: 0,
        heading: 0,
        headingAccuracy: 0,
        speed: 0,
        speedAccuracy: 0,
      ),
    );
  });

  setUp(() {
    sessionCubit = SessionCubit();
    authRepository = MockAuthRepository();
    geolocationService = MockGeolocationService();
    customerCatalogRepository = MockCustomerCatalogRepository();
    customerCartRepository = MockCustomerCartRepository();
    customerOrderRepository = MockCustomerOrderRepository();
    customerProfileRepository = MockCustomerProfileRepository();
    customerNotificationRepository = MockCustomerNotificationRepository();
    customerAiRepository = MockCustomerAiRepository();
    merchantRestaurantRepository = MockMerchantRestaurantRepository();
    merchantCatalogRepository = MockMerchantCatalogRepository();
    merchantOrderRepository = MockMerchantOrderRepository();
    merchantReviewRepository = MockMerchantReviewRepository();
    merchantRevenueRepository = MockMerchantRevenueRepository();
    merchantNotificationRepository = MockMerchantNotificationRepository();

    when(
      () => geolocationService.isLocationServiceEnabled(),
    ).thenAnswer((_) async => true);
    when(
      () => geolocationService.requestPermission(),
    ).thenAnswer((_) async => LocationPermission.whileInUse);
    when(
      () => geolocationService.getCurrentPosition(
        accuracy: any(named: 'accuracy'),
        timeLimit: any(named: 'timeLimit'),
      ),
    ).thenAnswer(
      (_) async => Position(
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
      ),
    );
    when(
      () => geolocationService.getFriendlyAddress(any()),
    ).thenAnswer((_) async => 'District 1');
    when(
      () => customerCatalogRepository.nearbyRestaurants(
        lat: any(named: 'lat'),
        lng: any(named: 'lng'),
        radiusKm: any(named: 'radiusKm'),
        sort: any(named: 'sort'),
        page: any(named: 'page'),
        size: any(named: 'size'),
      ),
    ).thenAnswer((_) async => restaurantSearchPage());

    when(
      () => merchantRestaurantRepository.listRestaurants(),
    ).thenAnswer((_) async => [merchantRestaurant()]);
    when(
      () => merchantOrderRepository.listRestaurantOrders(any()),
    ).thenAnswer((_) async => []);
    when(
      () => merchantRevenueRepository.getRevenueReport(
        from: any(named: 'from'),
        to: any(named: 'to'),
        topItems: any(named: 'topItems'),
      ),
    ).thenAnswer((_) async => merchantRevenueReport());
    when(
      () => merchantReviewRepository.listRestaurantReviews(any()),
    ).thenAnswer((_) async => []);
    when(
      () => merchantNotificationRepository.listNotifications(
        page: any(named: 'page'),
        size: any(named: 'size'),
      ),
    ).thenAnswer((_) async => []);
  });

  tearDown(() async {
    await sessionCubit.close();
  });

  Future<GoRouter> pumpRouter(WidgetTester tester) async {
    final router = AppRouter(sessionCubit).router;
    await tester.pumpWidget(
      MultiRepositoryProvider(
        providers: [
          RepositoryProvider<AuthRepository>.value(value: authRepository),
          RepositoryProvider<GeolocationService>.value(
            value: geolocationService,
          ),
          RepositoryProvider<CustomerCatalogRepository>.value(
            value: customerCatalogRepository,
          ),
          RepositoryProvider<CustomerCartRepository>.value(
            value: customerCartRepository,
          ),
          RepositoryProvider<CustomerOrderRepository>.value(
            value: customerOrderRepository,
          ),
          RepositoryProvider<CustomerProfileRepository>.value(
            value: customerProfileRepository,
          ),
          RepositoryProvider<CustomerNotificationRepository>.value(
            value: customerNotificationRepository,
          ),
          RepositoryProvider<CustomerAiRepository>.value(
            value: customerAiRepository,
          ),
          RepositoryProvider<MerchantRestaurantRepository>.value(
            value: merchantRestaurantRepository,
          ),
          RepositoryProvider<MerchantCatalogRepository>.value(
            value: merchantCatalogRepository,
          ),
          RepositoryProvider<MerchantOrderRepository>.value(
            value: merchantOrderRepository,
          ),
          RepositoryProvider<MerchantReviewRepository>.value(
            value: merchantReviewRepository,
          ),
          RepositoryProvider<MerchantRevenueRepository>.value(
            value: merchantRevenueRepository,
          ),
          RepositoryProvider<MerchantNotificationRepository>.value(
            value: merchantNotificationRepository,
          ),
        ],
        child: MultiBlocProvider(
          providers: [
            BlocProvider<SessionCubit>.value(value: sessionCubit),
            BlocProvider<LoginCubit>(
              create:
                  (_) => LoginCubit(
                    authRepository: authRepository,
                    sessionCubit: sessionCubit,
                  ),
            ),
          ],
          child: FoodyaMobileApp(router: router),
        ),
      ),
    );
    return router;
  }

  testWidgets('unauthenticated users redirect to login', (tester) async {
    sessionCubit.signOut();
    final router = await pumpRouter(tester);

    await tester.pumpAndSettle();

    expect(router.state.matchedLocation, '/login');
    expect(find.text('Login'), findsOneWidget);
  });

  testWidgets('authenticated customer lands on customer home', (tester) async {
    sessionCubit.signInAs(UserRole.customer);
    final router = await pumpRouter(tester);

    await tester.pumpAndSettle();

    expect(router.state.matchedLocation, '/customer/home');
    expect(find.text('Browse food'), findsOneWidget);
  });

  testWidgets('customer cannot access merchant routes', (tester) async {
    sessionCubit.signInAs(UserRole.customer);
    final router = await pumpRouter(tester);
    await tester.pumpAndSettle();

    router.go('/merchant/orders');
    await tester.pumpAndSettle();

    expect(router.state.matchedLocation, '/customer/home');
  });

  testWidgets('merchant cannot access customer routes', (tester) async {
    sessionCubit.signInAs(UserRole.merchant);
    final router = await pumpRouter(tester);
    await tester.pumpAndSettle();

    router.go('/customer/cart');
    await tester.pumpAndSettle();

    expect(router.state.matchedLocation, '/merchant/home');
    expect(find.text('Dashboard'), findsAtLeastNWidgets(1));
  });

  testWidgets('merchant auxiliary routes render inside merchant shell', (
    tester,
  ) async {
    sessionCubit.signInAs(UserRole.merchant);
    final router = await pumpRouter(tester);
    await tester.pumpAndSettle();

    router.go('/merchant/restaurant');
    await tester.pumpAndSettle();
    expect(router.state.matchedLocation, '/merchant/restaurant');
    expect(find.text('Restaurant Console'), findsOneWidget);
    expect(find.byType(NavigationBar), findsOneWidget);

    router.go('/merchant/reviews');
    await tester.pumpAndSettle();
    expect(router.state.matchedLocation, '/merchant/reviews');
    expect(find.text('Review Center'), findsOneWidget);
    expect(find.byType(NavigationBar), findsOneWidget);

    router.go('/merchant/notifications');
    await tester.pumpAndSettle();
    expect(router.state.matchedLocation, '/merchant/notifications');
    expect(find.text('Notifications'), findsOneWidget);
    expect(find.byType(NavigationBar), findsOneWidget);
  });
}
