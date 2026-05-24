import '../models/auth_session.dart';
import '../models/login_request.dart';
import '../models/password_recovery.dart';
import '../models/register_request.dart';

abstract class AuthRepository {
  Future<AuthSession> register(RegisterRequest request);

  Future<AuthSession> login(LoginRequest request);

  Future<AuthSession?> restoreSession();

  Future<AuthSession> refreshSession();

  Future<void> logoutAll();

  Future<ForgotPasswordResult> forgotPassword(String email);

  Future<VerifyOtpResult> verifyOtp({
    required String challengeToken,
    required String otp,
  });

  Future<void> resetPassword({
    required String resetToken,
    required String newPassword,
    required String confirmPassword,
  });

  Future<void> clearSession();
}
