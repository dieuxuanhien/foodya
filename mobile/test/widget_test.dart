import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter/material.dart';

import 'package:foodya_mobile/app.dart';
import 'package:foodya_mobile/core/auth/session_cubit.dart';
import 'package:foodya_mobile/core/auth/user_role.dart';
import 'package:foodya_mobile/core/router/app_router.dart';
import 'package:foodya_mobile/core/ui/foodya_shell.dart';
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
    expect(find.textContaining('SRS'), findsNothing);
    expect(find.textContaining('FR'), findsNothing);
  });

  testWidgets('login form shows validation copy for empty submit', (
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
    await tester.tap(find.text('Sign In'));
    await tester.pumpAndSettle();

    expect(find.text('Username or email is required.'), findsOneWidget);
    expect(find.text('Password is required.'), findsOneWidget);
  });

  testWidgets('customer shell shows customer bottom navigation', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: FoodyaRoleShell(
          role: UserRole.customer,
          location: '/customer/home',
          child: SizedBox.shrink(),
        ),
      ),
    );

    expect(find.text('Home'), findsOneWidget);
    expect(find.text('Browse'), findsOneWidget);
    expect(find.text('Cart'), findsOneWidget);
    expect(find.text('Orders'), findsOneWidget);
    expect(find.text('Account'), findsOneWidget);
  });

  testWidgets('merchant shell shows merchant bottom navigation', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: FoodyaRoleShell(
          role: UserRole.merchant,
          location: '/merchant/home',
          child: SizedBox.shrink(),
        ),
      ),
    );

    expect(find.text('Dashboard'), findsOneWidget);
    expect(find.text('Orders'), findsOneWidget);
    expect(find.text('Catalog'), findsOneWidget);
    expect(find.text('Insights'), findsOneWidget);
    expect(find.text('Account'), findsOneWidget);
  });
}
