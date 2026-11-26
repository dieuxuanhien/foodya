import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:foodya_mobile/core/auth/auth_session_recovery.dart';
import 'package:foodya_mobile/core/auth/auth_tokens.dart';
import 'package:foodya_mobile/core/auth/token_store.dart';
import 'package:foodya_mobile/core/network/api_exception.dart';

void main() {
  test('parallel 401 reuses tokens refreshed by another request', () async {
    const oldTokens = AuthTokens(
      accessToken: 'old-access',
      refreshToken: 'old-refresh',
    );
    const newTokens = AuthTokens(
      accessToken: 'new-access',
      refreshToken: 'new-refresh',
    );
    final store = _MemoryTokenStore(oldTokens);
    final refreshStarted = Completer<void>();
    final refreshResult = Completer<AuthTokens>();
    var refreshCalls = 0;
    final recovery = AuthSessionRecovery(
      tokenStore: store,
      refreshTokenExchange: (refreshToken) {
        refreshCalls += 1;
        expect(refreshToken, oldTokens.refreshToken);
        refreshStarted.complete();
        return refreshResult.future;
      },
    );
    final delayedUnauthorized = Completer<String>();
    final secondCallStarted = Completer<void>();
    var secondCallAttempts = 0;

    final first = recovery.runAuthorized((accessToken) async {
      if (accessToken == oldTokens.accessToken) {
        throw const ApiException(statusCode: 401, message: 'Expired');
      }
      return accessToken;
    });
    await refreshStarted.future;

    final second = recovery.runAuthorized((accessToken) async {
      secondCallAttempts += 1;
      if (secondCallAttempts == 1) {
        expect(accessToken, oldTokens.accessToken);
        secondCallStarted.complete();
        return delayedUnauthorized.future;
      }
      return accessToken;
    });

    await secondCallStarted.future;
    refreshResult.complete(newTokens);
    expect(await first, newTokens.accessToken);
    delayedUnauthorized.completeError(
      const ApiException(statusCode: 401, message: 'Expired'),
    );

    expect(await second, newTokens.accessToken);
    expect(refreshCalls, 1);
    expect(await store.read(), newTokens);
  });

  test('stale rejected refresh cannot clear newer rotated tokens', () async {
    const oldTokens = AuthTokens(
      accessToken: 'old-access',
      refreshToken: 'old-refresh',
    );
    const newTokens = AuthTokens(
      accessToken: 'new-access',
      refreshToken: 'new-refresh',
    );
    final store = _MemoryTokenStore(oldTokens);
    final exchangeResult = Completer<AuthTokens>();
    final recovery = AuthSessionRecovery(
      tokenStore: store,
      refreshTokenExchange: (_) => exchangeResult.future,
    );

    final refresh = recovery.refreshNow();
    await store.save(newTokens);
    exchangeResult.completeError(
      const ApiException(statusCode: 401, message: 'Refresh token rotated'),
    );

    expect(await refresh, newTokens);
    expect(await store.read(), newTokens);
  });
}

class _MemoryTokenStore implements TokenStore {
  _MemoryTokenStore(this._tokens);

  AuthTokens? _tokens;

  @override
  Future<void> clear() async {
    _tokens = null;
  }

  @override
  Future<AuthTokens?> read() async => _tokens;

  @override
  Future<void> save(AuthTokens tokens) async {
    _tokens = tokens;
  }
}
