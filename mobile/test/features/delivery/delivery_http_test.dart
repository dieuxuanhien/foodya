import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:foodya_mobile/core/auth/auth_session_recovery.dart';
import 'package:foodya_mobile/core/auth/auth_tokens.dart';
import 'package:foodya_mobile/core/auth/token_store.dart';
import 'package:foodya_mobile/core/network/api_exception.dart';
import 'package:foodya_mobile/features/delivery/data/data_sources/delivery_remote_data_source.dart';
import 'package:foodya_mobile/features/delivery/data/repositories/http_delivery_repository.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

void main() {
  const tokens = AuthTokens(accessToken: 'access', refreshToken: 'refresh');

  test('remote source sends verified Delivery paths and payloads', () async {
    final requests = <http.Request>[];
    final source = DeliveryRemoteDataSource(
      baseUrl: 'http://localhost:8080',
      client: MockClient((request) async {
        requests.add(request);
        final data = switch (request.url.path) {
          '/api/v1/delivery/status' ||
          '/api/v1/delivery/status/online' ||
          '/api/v1/delivery/status/offline' ||
          '/api/v1/delivery/status/location' => _sessionJson,
          '/api/v1/delivery/orders/assignments' => <Object?>[],
          '/api/v1/delivery/orders/order-1/accept' ||
          '/api/v1/delivery/orders/order-1/reject' ||
          '/api/v1/delivery/orders/order-1/status' => _orderJson,
          '/api/v1/delivery/batches/active' ||
          '/api/v1/delivery/batches/batch-1' => _batchJson,
          '/api/v1/delivery/orders/order-1/route' => _routeJson,
          '/api/v1/delivery/earnings' => _earningsJson,
          _ => <String, Object?>{},
        };
        return http.Response(
          jsonEncode({'data': data, 'traceId': 'trace'}),
          200,
        );
      }),
    );

    await source.getSession('access');
    await source.goOnline('access', lat: 10.78, lng: 106.70);
    await source.goOffline('access');
    await source.updateLocation('access', 10.79, 106.71);
    await source.getAssignments('access');
    await source.acceptAssignment('access', 'order-1');
    await source.rejectAssignment('access', 'order-1');
    await source.updateOrderStatus('access', 'order-1', 'DELIVERING');
    await source.getActiveBatch('access');
    await source.getBatch('access', 'batch-1');
    await source.markPickup('access', 'batch-1', 'order-1');
    await source.markDelivered('access', 'batch-1', 'order-1');
    await source.markFailed(
      'access',
      'batch-1',
      'order-1',
      reason: 'customer unavailable',
    );
    await source.getRoute('access', 'order-1');
    await source.getEarnings(
      'access',
      from: DateTime.utc(2026, 10),
      to: DateTime.utc(2026, 11),
    );

    expect(requests, hasLength(15));
    expect(
      requests.every((r) => r.headers['authorization'] == 'Bearer access'),
      isTrue,
    );
    expect(jsonDecode(requests[1].body), {'lat': 10.78, 'lng': 106.7});
    expect(jsonDecode(requests[7].body), {'status': 'DELIVERING'});
    expect(requests[12].url.queryParameters['reason'], 'customer unavailable');
    expect(requests.last.url.queryParameters.keys, containsAll(['from', 'to']));
  });

  test(
    'remote source preserves validation errors and malformed envelopes',
    () async {
      final validation = DeliveryRemoteDataSource(
        baseUrl: 'http://localhost:8080',
        client: MockClient(
          (_) async => http.Response(
            jsonEncode({
              'code': 'VALIDATION_ERROR',
              'message': 'invalid transition',
              'details': {'status': 'must be DELIVERING'},
            }),
            422,
          ),
        ),
      );
      await expectLater(
        validation.getSession('access'),
        throwsA(
          isA<ApiException>()
              .having((e) => e.statusCode, 'status', 422)
              .having((e) => e.details, 'details', isNotNull),
        ),
      );

      final malformed = DeliveryRemoteDataSource(
        baseUrl: 'http://localhost:8080',
        client: MockClient(
          (_) async => http.Response(jsonEncode({'data': {}}), 200),
        ),
      );
      await expectLater(
        malformed.getAssignments('access'),
        throwsA(
          isA<ApiException>().having(
            (e) => e.code,
            'code',
            'MALFORMED_RESPONSE',
          ),
        ),
      );
    },
  );

  test('remote source maps timeout without swallowing it', () async {
    final source = DeliveryRemoteDataSource(
      baseUrl: 'http://localhost:8080',
      requestTimeout: const Duration(milliseconds: 1),
      client: MockClient((_) async {
        await Future<void>.delayed(const Duration(milliseconds: 20));
        return http.Response('{}', 200);
      }),
    );
    await expectLater(
      source.getSession('access'),
      throwsA(
        isA<ApiException>().having((e) => e.code, 'code', 'NETWORK_TIMEOUT'),
      ),
    );
  });

  test('repository maps only no-active-batch 404 to null', () async {
    final repository = _repository(
      tokens,
      (_) async => http.Response(
        jsonEncode({
          'code': 'RESOURCE_NOT_FOUND',
          'message': 'No active batch found for driver',
        }),
        404,
      ),
    );
    expect(await repository.getActiveBatch(), isNull);
    await expectLater(
      repository.getBatch('missing'),
      throwsA(isA<ApiException>()),
    );
  });

  test(
    'batch mutation refetches detail and earnings parse as live report',
    () async {
      var batchGets = 0;
      final repository = _repository(tokens, (request) async {
        if (request.method == 'POST') {
          return http.Response(
            jsonEncode({
              'data': {'id': 'item-1'},
            }),
            200,
          );
        }
        if (request.url.path.contains('/batches/')) {
          batchGets++;
          return http.Response(jsonEncode({'data': _batchJson}), 200);
        }
        return http.Response(jsonEncode({'data': _earningsJson}), 200);
      });

      final batch = await repository.markPickup('batch-1', 'order-1');
      final earnings = await repository.getEarnings();
      expect(batch.batchId, 'batch-1');
      expect(batchGets, 1);
      expect(earnings.totalEarnings, 27000);
    },
  );

  test(
    'final unauthorized response invokes session-expired callback',
    () async {
      var callbackCount = 0;
      final store = _MemoryTokenStore(tokens);
      final repository = HttpDeliveryRepository(
        remoteDataSource: DeliveryRemoteDataSource(
          baseUrl: 'http://localhost:8080',
          client: MockClient(
            (_) async => http.Response(
              jsonEncode({'code': 'UNAUTHORIZED', 'message': 'expired'}),
              401,
            ),
          ),
        ),
        sessionRecovery: AuthSessionRecovery(
          tokenStore: store,
          refreshTokenExchange:
              (_) async => const AuthTokens(
                accessToken: 'new-access',
                refreshToken: 'new-refresh',
              ),
        ),
        onSessionExpired: () => callbackCount++,
      );

      await expectLater(repository.getSession(), throwsA(isA<ApiException>()));
      expect(callbackCount, 1);
      expect(await store.read(), isNull);
    },
  );
}

HttpDeliveryRepository _repository(
  AuthTokens tokens,
  Future<http.Response> Function(http.Request) handler,
) {
  return HttpDeliveryRepository(
    remoteDataSource: DeliveryRemoteDataSource(
      baseUrl: 'http://localhost:8080',
      client: MockClient(handler),
    ),
    sessionRecovery: AuthSessionRecovery(
      tokenStore: _MemoryTokenStore(tokens),
      refreshTokenExchange: (_) async => tokens,
    ),
  );
}

class _MemoryTokenStore implements TokenStore {
  _MemoryTokenStore(this._tokens);
  AuthTokens? _tokens;

  @override
  Future<void> clear() async => _tokens = null;

  @override
  Future<AuthTokens?> read() async => _tokens;

  @override
  Future<void> save(AuthTokens tokens) async => _tokens = tokens;
}

const _sessionJson = <String, Object?>{
  'sessionId': 'session-1',
  'status': 'ONLINE',
  'activeOrderCount': 1,
  'maxConcurrentOrders': 3,
};

const _orderJson = <String, Object?>{
  'orderId': 'order-1',
  'orderCode': 'FY-001',
  'customerName': 'Customer',
  'restaurantName': 'Restaurant',
  'status': 'ASSIGNED',
  'paymentStatus': 'UNPAID',
  'totalAmount': 99000,
};

const _batchJson = <String, Object?>{
  'batchId': 'batch-1',
  'status': 'ACTIVE',
  'pickupSequence': '[{"orderId":"order-1","lat":10.7,"lng":106.7}]',
  'deliverySequence': '[]',
  'items': <Object?>[],
};

const _routeJson = <String, Object?>{
  'distanceMeters': 1200,
  'durationSeconds': 600,
  'waypoints': <Object?>[],
};

const _earningsJson = <String, Object?>{
  'from': '2026-10-01T00:00:00Z',
  'to': '2026-11-01T00:00:00Z',
  'totalEarnings': 27000,
  'codCashCollected': 99000,
  'deliveryCount': 1,
  'entries': <Object?>[],
};
