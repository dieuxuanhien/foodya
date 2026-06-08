import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter/material.dart';

import 'package:foodya_mobile/app.dart';
import 'package:foodya_mobile/core/auth/session_cubit.dart';
import 'package:foodya_mobile/core/auth/user_role.dart';
import 'package:foodya_mobile/core/router/app_router.dart';
import 'package:foodya_mobile/core/ui/foodya_shell.dart';
import 'package:foodya_mobile/features/auth/data/repositories/mock_auth_repository.dart';
import 'package:foodya_mobile/features/auth/domain/models/password_recovery.dart';
import 'package:foodya_mobile/features/auth/domain/repositories/auth_repository.dart';
import 'package:foodya_mobile/features/auth/presentation/cubit/login_cubit.dart';
import 'package:foodya_mobile/features/auth/presentation/pages/password_recovery_page.dart';
import 'package:mocktail/mocktail.dart';

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

  testWidgets('password recovery ignores OTP result after route is disposed', (
    WidgetTester tester,
  ) async {
    final authRepository = _SlowAuthRepository();
    final request = Completer<ForgotPasswordResult>();
    when(
      () => authRepository.forgotPassword(any()),
    ).thenAnswer((_) => request.future);

    await tester.pumpWidget(
      RepositoryProvider<AuthRepository>.value(
        value: authRepository,
        child: const MaterialApp(home: PasswordRecoveryPage()),
      ),
    );

    await tester.enterText(find.byType(TextField).first, 'user@example.com');
    await tester.tap(find.text('Send OTP'));
    await tester.pump();

    await tester.pumpWidget(const MaterialApp(home: SizedBox.shrink()));
    request.complete(
      const ForgotPasswordResult(
        challengeToken: 'challenge-token',
        deliveryHint: 'user@example.com',
      ),
    );
    await tester.pump();

    expect(tester.takeException(), isNull);
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

class _SlowAuthRepository extends Mock implements AuthRepository {}
