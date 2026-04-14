import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import 'app.dart';
import 'core/auth/session_cubit.dart';
import 'core/router/app_router.dart';
import 'features/auth/data/repositories/mock_auth_repository.dart';
import 'features/auth/domain/repositories/auth_repository.dart';
import 'features/auth/presentation/cubit/login_cubit.dart';

void main() {
  runApp(FoodyaMobileBootstrap(authRepository: MockAuthRepository()));
}

class FoodyaMobileBootstrap extends StatelessWidget {
  FoodyaMobileBootstrap({super.key, required this.authRepository});

  final AuthRepository authRepository;
  final SessionCubit _sessionCubit = SessionCubit();

  @override
  Widget build(BuildContext context) {
    return RepositoryProvider<AuthRepository>.value(
      value: authRepository,
      child: MultiBlocProvider(
        providers: [
          BlocProvider<SessionCubit>.value(value: _sessionCubit),
          BlocProvider<LoginCubit>(
            create:
                (context) => LoginCubit(
                  authRepository: context.read<AuthRepository>(),
                  sessionCubit: _sessionCubit,
                ),
          ),
        ],
        child: FoodyaMobileApp(router: AppRouter(_sessionCubit).router),
      ),
    );
  }
}
