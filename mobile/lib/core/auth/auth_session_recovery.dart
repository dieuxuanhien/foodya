import '../network/api_exception.dart';
import 'auth_tokens.dart';
import 'token_store.dart';

typedef RefreshTokenExchange = Future<AuthTokens> Function(String refreshToken);

class AuthSessionRecovery {
  AuthSessionRecovery({
    required TokenStore tokenStore,
    required RefreshTokenExchange refreshTokenExchange,
  }) : _tokenStore = tokenStore,
       _refreshTokenExchange = refreshTokenExchange;

  final TokenStore _tokenStore;
  final RefreshTokenExchange _refreshTokenExchange;

  Future<AuthTokens>? _refreshInFlight;

  Future<AuthTokens> refreshNow() async {
    final stored = await _tokenStore.read();
    if (stored == null) {
      throw const ApiException(
        statusCode: 401,
        code: 'NO_REFRESH_TOKEN',
        message: 'No stored refresh token found.',
      );
    }

    return _refreshWithSingleFlight(stored.refreshToken);
  }

  Future<T> runAuthorized<T>(
    Future<T> Function(String accessToken) action,
  ) async {
    final stored = await _tokenStore.read();
    if (stored == null) {
      throw const ApiException(
        statusCode: 401,
        code: 'NO_REFRESH_TOKEN',
        message: 'Session not found. Please log in again.',
      );
    }

    try {
      return await action(stored.accessToken);
    } on ApiException catch (error) {
      if (error.statusCode != 401) {
        rethrow;
      }
    }

    final refreshed = await _refreshWithSingleFlight(stored.refreshToken);
    try {
      return await action(refreshed.accessToken);
    } on ApiException catch (error) {
      if (error.statusCode != 401) {
        rethrow;
      }

      await _tokenStore.clear();
      throw const ApiException(
        statusCode: 401,
        code: 'SESSION_EXPIRED',
        message: 'Your session has expired. Please log in again.',
      );
    }
  }

  Future<AuthTokens> _refreshWithSingleFlight(String refreshToken) {
    final existing = _refreshInFlight;
    if (existing != null) {
      return existing;
    }

    final refreshFuture = _performRefresh(refreshToken);
    _refreshInFlight = refreshFuture;
    return refreshFuture;
  }

  Future<AuthTokens> _performRefresh(String refreshToken) async {
    try {
      final refreshed = await _refreshTokenExchange(refreshToken);
      await _tokenStore.save(refreshed);
      return refreshed;
    } on ApiException catch (error) {
      if (error.statusCode == 401) {
        await _tokenStore.clear();
        throw const ApiException(
          statusCode: 401,
          code: 'SESSION_EXPIRED',
          message: 'Your session has expired. Please log in again.',
        );
      }
      rethrow;
    } finally {
      _refreshInFlight = null;
    }
  }
}
