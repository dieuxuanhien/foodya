import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:http/http.dart' as http;

import 'app.dart';
import 'core/auth/session_cubit.dart';
import 'core/auth/token_store.dart';
import 'core/config/app_config.dart';
import 'core/router/app_router.dart';
import 'features/auth/data/data_sources/auth_remote_data_source.dart';
import 'features/auth/data/repositories/http_auth_repository.dart';
import 'features/auth/domain/repositories/auth_repository.dart';
import 'features/auth/presentation/cubit/login_cubit.dart';
import 'features/customer/data/data_sources/customer_catalog_remote_data_source.dart';
import 'features/customer/data/repositories/http_customer_catalog_repository.dart';
import 'features/customer/domain/repositories/customer_catalog_repository.dart';

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

  @override
  void initState() {
    super.initState();
    _sessionCubit = SessionCubit();

    _authRepository = HttpAuthRepository(
      remoteDataSource: AuthRemoteDataSource(
        baseUrl: AppConfig.apiBaseUrl,
        client: widget._httpClient,
      ),
      tokenStore: widget._tokenStore,
    );

    _customerCatalogRepository = HttpCustomerCatalogRepository(
      remoteDataSource: CustomerCatalogRemoteDataSource(
        baseUrl: AppConfig.apiBaseUrl,
        client: widget._httpClient,
      ),
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
        RepositoryProvider<CustomerCatalogRepository>.value(
          value: _customerCatalogRepository,
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
