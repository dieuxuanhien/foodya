import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:foodya_mobile/app.dart';
import 'package:foodya_mobile/core/auth/session_cubit.dart';
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

Future<void> pumpFoodyaApp(
  WidgetTester tester, {
  required SessionCubit sessionCubit,
  required AuthRepository authRepository,
  required GeolocationService geolocationService,
  required CustomerCatalogRepository customerCatalogRepository,
  required CustomerCartRepository customerCartRepository,
  required CustomerOrderRepository customerOrderRepository,
  required CustomerProfileRepository customerProfileRepository,
  required CustomerNotificationRepository customerNotificationRepository,
  required CustomerAiRepository customerAiRepository,
  required MerchantRestaurantRepository merchantRestaurantRepository,
  required MerchantCatalogRepository merchantCatalogRepository,
  required MerchantOrderRepository merchantOrderRepository,
  required MerchantReviewRepository merchantReviewRepository,
  required MerchantRevenueRepository merchantRevenueRepository,
  required MerchantNotificationRepository merchantNotificationRepository,
}) async {
  await tester.pumpWidget(
    MultiRepositoryProvider(
      providers: [
        RepositoryProvider<AuthRepository>.value(value: authRepository),
        RepositoryProvider<GeolocationService>.value(value: geolocationService),
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
                (context) => LoginCubit(
                  authRepository: context.read<AuthRepository>(),
                  sessionCubit: sessionCubit,
                ),
          ),
        ],
        child: FoodyaMobileApp(router: AppRouter(sessionCubit).router),
      ),
    ),
  );
}

Future<void> pumpTestMaterial(WidgetTester tester, Widget child) {
  return tester.pumpWidget(MaterialApp(home: child));
}
