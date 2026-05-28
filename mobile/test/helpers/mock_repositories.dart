import 'package:foodya_mobile/core/location/geolocation_service.dart';
import 'package:foodya_mobile/features/auth/domain/repositories/auth_repository.dart';
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
import 'package:mocktail/mocktail.dart';

class MockAuthRepository extends Mock implements AuthRepository {}

class MockGeolocationService extends Mock implements GeolocationService {}

class MockCustomerAiRepository extends Mock implements CustomerAiRepository {}

class MockCustomerCartRepository extends Mock
    implements CustomerCartRepository {}

class MockCustomerCatalogRepository extends Mock
    implements CustomerCatalogRepository {}

class MockCustomerNotificationRepository extends Mock
    implements CustomerNotificationRepository {}

class MockCustomerOrderRepository extends Mock
    implements CustomerOrderRepository {}

class MockCustomerProfileRepository extends Mock
    implements CustomerProfileRepository {}

class MockMerchantCatalogRepository extends Mock
    implements MerchantCatalogRepository {}

class MockMerchantNotificationRepository extends Mock
    implements MerchantNotificationRepository {}

class MockMerchantOrderRepository extends Mock
    implements MerchantOrderRepository {}

class MockMerchantRestaurantRepository extends Mock
    implements MerchantRestaurantRepository {}

class MockMerchantRevenueRepository extends Mock
    implements MerchantRevenueRepository {}

class MockMerchantReviewRepository extends Mock
    implements MerchantReviewRepository {}
