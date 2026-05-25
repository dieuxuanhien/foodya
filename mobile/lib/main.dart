import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:http/http.dart' as http;

import 'app.dart';
import 'core/auth/session_cubit.dart';
import 'core/auth/auth_session_recovery.dart';
import 'core/auth/token_store.dart';
import 'core/config/app_config.dart';
import 'core/router/app_router.dart';
import 'core/location/location_address_remote_data_source.dart';
import 'features/auth/data/data_sources/auth_remote_data_source.dart';
import 'features/auth/data/repositories/http_auth_repository.dart';
import 'features/auth/domain/repositories/auth_repository.dart';
import 'features/auth/presentation/cubit/login_cubit.dart';
import 'features/customer/data/data_sources/customer_cart_remote_data_source.dart';
import 'features/customer/data/data_sources/customer_ai_remote_data_source.dart';
import 'features/customer/data/data_sources/customer_notification_remote_data_source.dart';
import 'features/customer/data/data_sources/customer_order_remote_data_source.dart';
import 'features/customer/data/data_sources/customer_profile_remote_data_source.dart';
import 'features/customer/data/repositories/http_customer_ai_repository.dart';
import 'features/customer/data/repositories/http_customer_cart_repository.dart';
import 'features/customer/data/repositories/http_customer_notification_repository.dart';
import 'features/customer/data/repositories/http_customer_order_repository.dart';
import 'features/customer/data/repositories/http_customer_profile_repository.dart';
import 'features/customer/data/data_sources/customer_catalog_remote_data_source.dart';
import 'features/customer/data/repositories/http_customer_catalog_repository.dart';
import 'features/customer/domain/repositories/customer_ai_repository.dart';
import 'features/customer/domain/repositories/customer_cart_repository.dart';
import 'features/customer/domain/repositories/customer_notification_repository.dart';
import 'features/customer/domain/repositories/customer_order_repository.dart';
import 'features/customer/domain/repositories/customer_catalog_repository.dart';
import 'features/customer/domain/repositories/customer_profile_repository.dart';
import 'features/merchant/data/data_sources/merchant_restaurant_remote_data_source.dart';
import 'features/merchant/data/data_sources/merchant_catalog_remote_data_source.dart';
import 'features/merchant/data/data_sources/merchant_order_remote_data_source.dart';
import 'features/merchant/data/data_sources/merchant_review_remote_data_source.dart';
import 'features/merchant/data/data_sources/merchant_revenue_remote_data_source.dart';
import 'features/merchant/data/data_sources/merchant_notification_remote_data_source.dart';
import 'features/merchant/data/repositories/http_merchant_catalog_repository.dart';
import 'features/merchant/data/repositories/http_merchant_order_repository.dart';
import 'features/merchant/data/repositories/http_merchant_restaurant_repository.dart';
import 'features/merchant/data/repositories/http_merchant_review_repository.dart';
import 'features/merchant/data/repositories/http_merchant_revenue_repository.dart';
import 'features/merchant/data/repositories/http_merchant_notification_repository.dart';
import 'features/merchant/domain/repositories/merchant_catalog_repository.dart';
import 'features/merchant/domain/repositories/merchant_order_repository.dart';
import 'features/merchant/domain/repositories/merchant_restaurant_repository.dart';
import 'features/merchant/domain/repositories/merchant_review_repository.dart';
import 'features/merchant/domain/repositories/merchant_revenue_repository.dart';
import 'features/merchant/domain/repositories/merchant_notification_repository.dart';
import 'core/location/geolocation_service.dart';

void main() {
  runApp(FoodyaMobileBootstrap());
}

class FoodyaMobileBootstrap extends StatefulWidget {
  FoodyaMobileBootstrap({super.key});

  final http.Client _httpClient = http.Client();
  final TokenStore _tokenStore = SecureTokenStore();

  @override
  State<FoodyaMobileBootstrap> createState() => _FoodyaMobileBootstrapState();
}

class _FoodyaMobileBootstrapState extends State<FoodyaMobileBootstrap> {
  late final SessionCubit _sessionCubit;
  late final AuthRepository _authRepository;
  late final CustomerCatalogRepository _customerCatalogRepository;
  late final CustomerCartRepository _customerCartRepository;
  late final CustomerOrderRepository _customerOrderRepository;
  late final CustomerProfileRepository _customerProfileRepository;
  late final CustomerNotificationRepository _customerNotificationRepository;
  late final CustomerAiRepository _customerAiRepository;
  late final MerchantRestaurantRepository _merchantRestaurantRepository;
  late final MerchantCatalogRepository _merchantCatalogRepository;
  late final MerchantOrderRepository _merchantOrderRepository;
  late final MerchantReviewRepository _merchantReviewRepository;
  late final MerchantRevenueRepository _merchantRevenueRepository;
  late final MerchantNotificationRepository _merchantNotificationRepository;
  late final GeolocationService _geolocationService;
  late final AuthRemoteDataSource _authRemoteDataSource;
  late final AuthSessionRecovery _authSessionRecovery;

  @override
  void initState() {
    super.initState();
    _sessionCubit = SessionCubit();

    _authRemoteDataSource = AuthRemoteDataSource(
      baseUrl: AppConfig.apiBaseUrl,
      client: widget._httpClient,
    );

    _authRepository = HttpAuthRepository(
      remoteDataSource: _authRemoteDataSource,
      tokenStore: widget._tokenStore,
    );

    _authSessionRecovery = AuthSessionRecovery(
      tokenStore: widget._tokenStore,
      refreshTokenExchange: _authRemoteDataSource.refresh,
    );

    _customerCatalogRepository = HttpCustomerCatalogRepository(
      remoteDataSource: CustomerCatalogRemoteDataSource(
        baseUrl: AppConfig.apiBaseUrl,
        client: widget._httpClient,
      ),
    );

    _customerCartRepository = HttpCustomerCartRepository(
      remoteDataSource: CustomerCartRemoteDataSource(
        baseUrl: AppConfig.apiBaseUrl,
        client: widget._httpClient,
      ),
      sessionRecovery: _authSessionRecovery,
    );

    _customerOrderRepository = HttpCustomerOrderRepository(
      remoteDataSource: CustomerOrderRemoteDataSource(
        baseUrl: AppConfig.apiBaseUrl,
        client: widget._httpClient,
      ),
      sessionRecovery: _authSessionRecovery,
    );

    _customerProfileRepository = HttpCustomerProfileRepository(
      remoteDataSource: CustomerProfileRemoteDataSource(
        baseUrl: AppConfig.apiBaseUrl,
        client: widget._httpClient,
      ),
      sessionRecovery: _authSessionRecovery,
    );

    _customerNotificationRepository = HttpCustomerNotificationRepository(
      remoteDataSource: CustomerNotificationRemoteDataSource(
        baseUrl: AppConfig.apiBaseUrl,
        client: widget._httpClient,
      ),
      sessionRecovery: _authSessionRecovery,
    );

    _customerAiRepository = HttpCustomerAiRepository(
      remoteDataSource: CustomerAiRemoteDataSource(
        baseUrl: AppConfig.apiBaseUrl,
        client: widget._httpClient,
      ),
      sessionRecovery: _authSessionRecovery,
    );

    _merchantRestaurantRepository = HttpMerchantRestaurantRepository(
      remoteDataSource: MerchantRestaurantRemoteDataSource(
        baseUrl: AppConfig.apiBaseUrl,
        client: widget._httpClient,
      ),
      sessionRecovery: _authSessionRecovery,
    );

    _merchantCatalogRepository = HttpMerchantCatalogRepository(
      remoteDataSource: MerchantCatalogRemoteDataSource(
        baseUrl: AppConfig.apiBaseUrl,
        client: widget._httpClient,
      ),
      sessionRecovery: _authSessionRecovery,
    );

    _merchantOrderRepository = HttpMerchantOrderRepository(
      remoteDataSource: MerchantOrderRemoteDataSource(
        baseUrl: AppConfig.apiBaseUrl,
        client: widget._httpClient,
      ),
      sessionRecovery: _authSessionRecovery,
    );

    _merchantReviewRepository = HttpMerchantReviewRepository(
      remoteDataSource: MerchantReviewRemoteDataSource(
        baseUrl: AppConfig.apiBaseUrl,
        client: widget._httpClient,
      ),
      sessionRecovery: _authSessionRecovery,
    );

    _merchantRevenueRepository = HttpMerchantRevenueRepository(
      remoteDataSource: MerchantRevenueRemoteDataSource(
        baseUrl: AppConfig.apiBaseUrl,
        client: widget._httpClient,
      ),
      sessionRecovery: _authSessionRecovery,
    );

    _merchantNotificationRepository = HttpMerchantNotificationRepository(
      remoteDataSource: MerchantNotificationRemoteDataSource(
        baseUrl: AppConfig.apiBaseUrl,
        client: widget._httpClient,
      ),
      sessionRecovery: _authSessionRecovery,
    );

    final locationRemoteDataSource = LocationAddressRemoteDataSource(
      baseUrl: AppConfig.apiBaseUrl,
      client: widget._httpClient,
    );

    _geolocationService = GeolocationService(
      remoteDataSource: locationRemoteDataSource,
      sessionRecovery: _authSessionRecovery,
    );
  }

  @override
  void dispose() {
    _sessionCubit.close();
    widget._httpClient.close();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MultiRepositoryProvider(
      providers: [
        RepositoryProvider<AuthRepository>.value(value: _authRepository),
        RepositoryProvider<GeolocationService>(
          create: (_) => _geolocationService,
        ),
        RepositoryProvider<CustomerCatalogRepository>.value(
          value: _customerCatalogRepository,
        ),
        RepositoryProvider<CustomerCartRepository>.value(
          value: _customerCartRepository,
        ),
        RepositoryProvider<CustomerOrderRepository>.value(
          value: _customerOrderRepository,
        ),
        RepositoryProvider<CustomerProfileRepository>.value(
          value: _customerProfileRepository,
        ),
        RepositoryProvider<CustomerNotificationRepository>.value(
          value: _customerNotificationRepository,
        ),
        RepositoryProvider<CustomerAiRepository>.value(
          value: _customerAiRepository,
        ),
        RepositoryProvider<MerchantRestaurantRepository>.value(
          value: _merchantRestaurantRepository,
        ),
        RepositoryProvider<MerchantCatalogRepository>.value(
          value: _merchantCatalogRepository,
        ),
        RepositoryProvider<MerchantOrderRepository>.value(
          value: _merchantOrderRepository,
        ),
        RepositoryProvider<MerchantReviewRepository>.value(
          value: _merchantReviewRepository,
        ),
        RepositoryProvider<MerchantRevenueRepository>.value(
          value: _merchantRevenueRepository,
        ),
        RepositoryProvider<MerchantNotificationRepository>.value(
          value: _merchantNotificationRepository,
        ),
      ],
      child: MultiBlocProvider(
        providers: [
          BlocProvider<SessionCubit>.value(value: _sessionCubit),
          BlocProvider<LoginCubit>(
            lazy: false,
            create:
                (context) => LoginCubit(
                  authRepository: context.read<AuthRepository>(),
                  sessionCubit: _sessionCubit,
                )..restoreSession(),
          ),
        ],
        child: FoodyaMobileApp(router: AppRouter(_sessionCubit).router),
      ),
    );
  }
}
