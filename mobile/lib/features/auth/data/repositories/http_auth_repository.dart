import '../../../../core/auth/auth_tokens.dart';
import '../../../../core/auth/jwt_claims_decoder.dart';
import '../../../../core/auth/token_store.dart';
import '../../../../core/network/api_exception.dart';
import '../../domain/models/auth_session.dart';
import '../../domain/models/login_request.dart';
import '../../domain/models/register_request.dart';
import '../../domain/repositories/auth_repository.dart';
import '../data_sources/auth_remote_data_source.dart';

class HttpAuthRepository implements AuthRepository {
  HttpAuthRepository({
    required AuthRemoteDataSource remoteDataSource,
    required TokenStore tokenStore,
  }) : _remoteDataSource = remoteDataSource,
       _tokenStore = tokenStore;

  final AuthRemoteDataSource _remoteDataSource;
  final TokenStore _tokenStore;

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
    final stored = await _tokenStore.read();
    if (stored == null) {
      throw const ApiException(
        statusCode: 401,
        code: 'NO_REFRESH_TOKEN',
        message: 'No stored refresh token found.',
      );
    }

    final refreshed = await _remoteDataSource.refresh(stored.refreshToken);
    return _persistSession(refreshed.accessToken, refreshed.refreshToken);
  }

  @override
  Future<void> logoutAll() async {
    final stored = await _tokenStore.read();
    if (stored != null) {
      try {
        await _remoteDataSource.logoutAll(stored.accessToken);
      } on ApiException catch (error) {
        if (error.statusCode != 401) {
          rethrow;
        }
      }
    }

    await _tokenStore.clear();
  }

  @override
  Future<void> clearSession() {
    return _tokenStore.clear();
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
