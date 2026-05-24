import 'dart:async';

import '../../../../core/auth/auth_tokens.dart';
import '../../domain/repositories/auth_repository.dart';
import '../../domain/models/auth_session.dart';
import '../../domain/models/login_request.dart';
import '../../domain/models/password_recovery.dart';
import '../../domain/models/register_request.dart';
import '../../../../core/auth/user_role.dart';

class MockAuthRepository implements AuthRepository {
  AuthSession? _session;

  @override
  Future<AuthSession> register(RegisterRequest request) async {
    await Future<void>.delayed(const Duration(milliseconds: 300));
    _session = AuthSession(
      role: request.role,
      tokens: const AuthTokens(
        accessToken: 'mock-access-token',
        refreshToken: 'mock-refresh-token',
      ),
    );
    return _session!;
  }

  @override
  Future<AuthSession> login(LoginRequest request) async {
    await Future<void>.delayed(const Duration(milliseconds: 250));
    _session = const AuthSession(
      role: UserRole.customer,
      tokens: AuthTokens(
        accessToken: 'mock-access-token',
        refreshToken: 'mock-refresh-token',
      ),
    );
    return _session!;
  }

  @override
  Future<AuthSession?> restoreSession() async {
    return _session;
  }

  @override
  Future<AuthSession> refreshSession() async {
    final current = _session;
    if (current == null) {
      throw StateError('No mock session to refresh');
    }
    return current;
  }

  @override
  Future<void> logoutAll() async {
    _session = null;
  }

  @override
  Future<void> clearSession() async {
    _session = null;
  }

  @override
  Future<ForgotPasswordResult> forgotPassword(String email) async {
    return const ForgotPasswordResult(
      challengeToken: 'mock-challenge',
      deliveryHint: 'mock@example.com',
    );
  }

  @override
  Future<VerifyOtpResult> verifyOtp({
    required String challengeToken,
    required String otp,
  }) async {
    return const VerifyOtpResult(resetToken: 'mock-reset');
  }

  @override
  Future<void> resetPassword({
    required String resetToken,
    required String newPassword,
    required String confirmPassword,
  }) async {}
}
