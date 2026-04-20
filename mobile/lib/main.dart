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
  }

  @override
  void dispose() {
    _sessionCubit.close();
    widget._httpClient.close();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return RepositoryProvider<AuthRepository>.value(
      value: _authRepository,
      child: MultiBlocProvider(
        providers: [
          BlocProvider<SessionCubit>.value(value: _sessionCubit),
          BlocProvider<LoginCubit>(
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
