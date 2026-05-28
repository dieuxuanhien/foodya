import 'package:bloc_test/bloc_test.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:foodya_mobile/core/auth/session_cubit.dart';
import 'package:foodya_mobile/core/auth/session_state.dart';
import 'package:foodya_mobile/core/auth/user_role.dart';
import 'package:foodya_mobile/core/network/api_exception.dart';
import 'package:foodya_mobile/features/auth/domain/models/login_request.dart';
import 'package:foodya_mobile/features/auth/domain/models/register_request.dart';
import 'package:foodya_mobile/features/auth/presentation/cubit/login_cubit.dart';
import 'package:foodya_mobile/features/auth/presentation/cubit/login_state.dart';
import 'package:mocktail/mocktail.dart';

import '../../helpers/mock_repositories.dart';
import '../../helpers/test_models.dart';

class _FakeLoginRequest extends Fake implements LoginRequest {}

class _FakeRegisterRequest extends Fake implements RegisterRequest {}

void main() {
  setUpAll(() {
    registerFallbackValue(_FakeLoginRequest());
    registerFallbackValue(_FakeRegisterRequest());
  });

  group('SessionCubit', () {
    blocTest<SessionCubit, SessionState>(
      'emits authenticated and unauthenticated states',
      build: SessionCubit.new,
      act: (cubit) {
        cubit.signInAs(UserRole.customer);
        cubit.signOut();
      },
      expect:
          () => const [
            SessionState.authenticated(UserRole.customer),
            SessionState.unauthenticated(),
          ],
    );
  });

  group('LoginCubit', () {
    late MockAuthRepository authRepository;
    late SessionCubit sessionCubit;

    LoginCubit buildCubit() {
      return LoginCubit(
        authRepository: authRepository,
        sessionCubit: sessionCubit,
      );
    }

    setUp(() {
      authRepository = MockAuthRepository();
      sessionCubit = SessionCubit();
    });

    test('restoreSession authenticates restored session', () async {
      when(
        () => authRepository.restoreSession(),
      ).thenAnswer((_) async => authSession(role: UserRole.merchant));
      final cubit = buildCubit();

      await cubit.restoreSession();

      expect(cubit.state.status, LoginStatus.idle);
      expect(cubit.state.infoMessage, contains('Merchant'));
      expect(
        sessionCubit.state,
        const SessionState.authenticated(UserRole.merchant),
      );
    });

    test('restoreSession signs out when no session exists', () async {
      when(() => authRepository.restoreSession()).thenAnswer((_) async => null);
      final cubit = buildCubit();

      await cubit.restoreSession();

      expect(cubit.state.status, LoginStatus.idle);
      expect(sessionCubit.state, const SessionState.unauthenticated());
    });

    test(
      'restoreSession clears bad session and signs out on failure',
      () async {
        when(() => authRepository.restoreSession()).thenThrow(Exception('bad'));
        when(() => authRepository.clearSession()).thenAnswer((_) async {});
        final cubit = buildCubit();

        await cubit.restoreSession();

        verify(() => authRepository.clearSession()).called(1);
        expect(sessionCubit.state, const SessionState.unauthenticated());
      },
    );

    test('signIn validates required fields', () async {
      final cubit = buildCubit();

      await cubit.signIn(usernameOrEmail: ' ', password: '');

      expect(cubit.state.status, LoginStatus.failure);
      expect(cubit.state.errorMessage, contains('required'));
      verifyNever(() => authRepository.login(any()));
    });

    test('signIn authenticates and maps API field errors', () async {
      when(
        () => authRepository.login(any()),
      ).thenAnswer((_) async => authSession(role: UserRole.customer));
      final cubit = buildCubit();

      await cubit.signIn(usernameOrEmail: ' alice ', password: 'secret');

      expect(cubit.state.status, LoginStatus.idle);
      expect(
        sessionCubit.state,
        const SessionState.authenticated(UserRole.customer),
      );

      when(() => authRepository.login(any())).thenThrow(
        const ApiException(
          statusCode: 400,
          message: 'Invalid',
          details: {'email': 'Email already exists'},
        ),
      );

      await cubit.signIn(usernameOrEmail: 'alice', password: 'bad');

      expect(cubit.state.status, LoginStatus.failure);
      expect(cubit.state.fieldErrors['email'], 'Email already exists');
    });

    test('register validates fields and signs in on success', () async {
      final cubit = buildCubit();

      await cubit.register(
        username: '',
        email: '',
        phoneNumber: '',
        fullName: '',
        password: '',
        role: UserRole.customer,
      );

      expect(cubit.state.status, LoginStatus.failure);
      expect(cubit.state.errorMessage, contains('required'));

      when(
        () => authRepository.register(any()),
      ).thenAnswer((_) async => authSession(role: UserRole.merchant));

      await cubit.register(
        username: 'merchant',
        email: 'm@example.com',
        phoneNumber: '0900000000',
        fullName: 'Merchant',
        password: 'secret',
        role: UserRole.merchant,
      );

      expect(
        sessionCubit.state,
        const SessionState.authenticated(UserRole.merchant),
      );
      expect(cubit.state.infoMessage, contains('Merchant'));
    });

    test('refreshToken signs out and clears session on unauthorized', () async {
      when(
        () => authRepository.refreshSession(),
      ).thenThrow(const ApiException(statusCode: 401, message: 'Expired'));
      when(() => authRepository.clearSession()).thenAnswer((_) async {});
      final cubit = buildCubit();

      await cubit.refreshToken();

      verify(() => authRepository.clearSession()).called(1);
      expect(sessionCubit.state, const SessionState.unauthenticated());
      expect(cubit.state.status, LoginStatus.failure);
    });

    test('logoutAll signs out on success', () async {
      when(() => authRepository.logoutAll()).thenAnswer((_) async {});
      final cubit = buildCubit();
      sessionCubit.signInAs(UserRole.customer);

      await cubit.logoutAll();

      expect(sessionCubit.state, const SessionState.unauthenticated());
      expect(cubit.state.infoMessage, contains('Logged out'));
    });
  });
}
