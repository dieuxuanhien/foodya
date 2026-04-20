import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import 'package:foodya_mobile/app.dart';
import 'package:foodya_mobile/core/auth/session_cubit.dart';
import 'package:foodya_mobile/core/router/app_router.dart';
import 'package:foodya_mobile/features/auth/data/repositories/mock_auth_repository.dart';
import 'package:foodya_mobile/features/auth/domain/repositories/auth_repository.dart';
import 'package:foodya_mobile/features/auth/presentation/cubit/login_cubit.dart';

void main() {
  testWidgets('shows login and register auth forms', (
    WidgetTester tester,
  ) async {
    final authRepository = MockAuthRepository();
    final sessionCubit = SessionCubit();
    sessionCubit.signOut();

    await tester.pumpWidget(
      RepositoryProvider<AuthRepository>.value(
        value: authRepository,
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

    await tester.pumpAndSettle();

    expect(find.text('Login'), findsOneWidget);
    expect(find.text('Register'), findsOneWidget);
    expect(find.text('Sign In'), findsOneWidget);
  });
}
