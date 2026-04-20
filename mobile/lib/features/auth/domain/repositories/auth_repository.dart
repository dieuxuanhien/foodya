import '../models/auth_session.dart';
import '../models/login_request.dart';
import '../models/register_request.dart';

abstract class AuthRepository {
  Future<AuthSession> register(RegisterRequest request);

  Future<AuthSession> login(LoginRequest request);

  Future<AuthSession?> restoreSession();

  Future<AuthSession> refreshSession();

  Future<void> logoutAll();

  Future<void> clearSession();
}
