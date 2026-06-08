import '../../../../core/auth/auth_tokens.dart';
import '../../../../core/auth/auth_session_recovery.dart';
import '../../../../core/auth/jwt_claims_decoder.dart';
import '../../../../core/auth/token_store.dart';
import '../../../../core/network/api_exception.dart';
import '../../domain/models/auth_session.dart';
import '../../domain/models/login_request.dart';
import '../../domain/models/password_recovery.dart';
import '../../domain/models/register_request.dart';
import '../../domain/repositories/auth_repository.dart';
import '../data_sources/auth_remote_data_source.dart';

class HttpAuthRepository implements AuthRepository {
  HttpAuthRepository({
    required AuthRemoteDataSource remoteDataSource,
    required TokenStore tokenStore,
  }) : _remoteDataSource = remoteDataSource,
       _tokenStore = tokenStore,
       _sessionRecovery = AuthSessionRecovery(
         tokenStore: tokenStore,
         refreshTokenExchange: remoteDataSource.refresh,
       );

  final AuthRemoteDataSource _remoteDataSource;
  final TokenStore _tokenStore;
  final AuthSessionRecovery _sessionRecovery;

  @override
  Future<AuthSession> register(RegisterRequest request) async {
    final tokens = await _remoteDataSource.register(request);
    return _persistSession(tokens.accessToken, tokens.refreshToken);
  }

  @override
  Future<AuthSession> login(LoginRequest request) async {
    final tokens = await _remoteDataSource.login(request);
    return _persistSession(tokens.accessToken, tokens.refreshToken);
  }

  @override
  Future<AuthSession?> restoreSession() async {
    final stored = await _tokenStore.read();
    if (stored == null) {
      return null;
    }

    if (JwtClaimsDecoder.isExpiringSoon(stored.accessToken)) {
      try {
        return await refreshSession();
      } catch (_) {
        await _tokenStore.clear();
        return null;
      }
    }

    final role = JwtClaimsDecoder.parseRole(stored.accessToken);
    if (role == null) {
      await _tokenStore.clear();
      return null;
    }

    return AuthSession(role: role, tokens: stored);
  }

  @override
  Future<AuthSession> refreshSession() async {
    final refreshed = await _sessionRecovery.refreshNow();
    return _persistSession(refreshed.accessToken, refreshed.refreshToken);
  }

  @override
  Future<void> logoutAll() async {
    try {
      await _sessionRecovery.runAuthorized((accessToken) {
        return _remoteDataSource.logoutAll(accessToken);
      });
    } on ApiException catch (error) {
      if (error.statusCode != 401) {
        rethrow;
      }
    }

    await _tokenStore.clear();
  }

  @override
  Future<void> clearSession() {
    return _tokenStore.clear();
  }

  @override
  Future<ForgotPasswordResult> forgotPassword(String email) {
    return _remoteDataSource.forgotPassword(email.trim());
  }

  @override
  Future<VerifyOtpResult> verifyOtp({
    required String challengeToken,
    required String otp,
  }) {
    return _remoteDataSource.verifyOtp(
      challengeToken: challengeToken,
      otp: otp,
    );
  }

  @override
  Future<void> resetPassword({
    required String resetToken,
    required String newPassword,
    required String confirmPassword,
  }) {
    return _remoteDataSource.resetPassword(
      resetToken: resetToken,
      newPassword: newPassword,
      confirmPassword: confirmPassword,
    );
  }

  Future<AuthSession> _persistSession(
    String accessToken,
    String refreshToken,
  ) async {
    final role = JwtClaimsDecoder.parseRole(accessToken);
    if (role == null) {
      await _tokenStore.clear();
      throw const ApiException(
        statusCode: 500,
        code: 'TOKEN_ROLE_MISSING',
        message: 'Access token does not include a supported role.',
      );
    }

    final tokens = AuthTokens(
      accessToken: accessToken,
      refreshToken: refreshToken,
    );
    await _tokenStore.save(tokens);

    return AuthSession(role: role, tokens: tokens);
  }
}
