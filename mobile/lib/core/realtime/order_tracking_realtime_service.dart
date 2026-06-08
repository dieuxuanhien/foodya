import 'dart:async';
import 'dart:convert';

import 'package:stomp_dart_client/stomp_dart_client.dart';

import '../../features/customer/domain/models/order_tracking_point.dart';
import '../auth/auth_session_recovery.dart';
import '../auth/jwt_claims_decoder.dart';
import '../auth/token_store.dart';

class OrderTrackingRealtimeUpdate {
  const OrderTrackingRealtimeUpdate._({this.point, this.isConnected});

  const OrderTrackingRealtimeUpdate.point(OrderTrackingPoint point)
    : this._(point: point);

  const OrderTrackingRealtimeUpdate.connected() : this._(isConnected: true);

  const OrderTrackingRealtimeUpdate.disconnected()
    : this._(isConnected: false);

  final OrderTrackingPoint? point;
  final bool? isConnected;
}

abstract class OrderTrackingRealtimeService {
  Stream<OrderTrackingRealtimeUpdate> watchOrderTracking(String orderId);

  Future<void> dispose();
}

class NoopOrderTrackingRealtimeService implements OrderTrackingRealtimeService {
  const NoopOrderTrackingRealtimeService();

  @override
  Stream<OrderTrackingRealtimeUpdate> watchOrderTracking(String orderId) {
    return Stream.value(const OrderTrackingRealtimeUpdate.disconnected());
  }

  @override
  Future<void> dispose() async {}
}

class StompOrderTrackingRealtimeService implements OrderTrackingRealtimeService {
  StompOrderTrackingRealtimeService({
    required String baseUrl,
    required TokenStore tokenStore,
    required AuthSessionRecovery sessionRecovery,
  }) : _baseUrl = baseUrl,
       _tokenStore = tokenStore,
       _sessionRecovery = sessionRecovery;

  final String _baseUrl;
  final TokenStore _tokenStore;
  final AuthSessionRecovery _sessionRecovery;

  StompClient? _client;
  void Function({Map<String, String>? unsubscribeHeaders})? _unsubscribe;

  @override
  Stream<OrderTrackingRealtimeUpdate> watchOrderTracking(String orderId) {
    late final StreamController<OrderTrackingRealtimeUpdate> controller;
    controller = StreamController<OrderTrackingRealtimeUpdate>(
      onListen: () => _connect(orderId, controller),
      onCancel: () async => _disconnect(),
    );
    return controller.stream;
  }

  @override
  Future<void> dispose() {
    return _disconnect();
  }

  Future<void> _connect(
    String orderId,
    StreamController<OrderTrackingRealtimeUpdate> controller,
  ) async {
    await _disconnect();
    try {
      final accessToken = await _accessToken();
      final authorization = 'Bearer $accessToken';
      late final StompClient client;
      client = StompClient(
        config: StompConfig(
          url: _webSocketUrl(),
          stompConnectHeaders: {'Authorization': authorization},
          webSocketConnectHeaders: {'Authorization': authorization},
          onConnect: (_) {
            if (controller.isClosed) {
              return;
            }
            controller.add(const OrderTrackingRealtimeUpdate.connected());
            _unsubscribe = client.subscribe(
              destination: '/user/queue/orders/$orderId/tracking',
              callback: (frame) {
                final point = _decodePoint(frame.body);
                if (point != null && !controller.isClosed) {
                  controller.add(OrderTrackingRealtimeUpdate.point(point));
                }
              },
            );
          },
          onDisconnect: (_) {
            if (!controller.isClosed) {
              controller.add(const OrderTrackingRealtimeUpdate.disconnected());
            }
          },
          onWebSocketError: (_) {
            if (!controller.isClosed) {
              controller.add(const OrderTrackingRealtimeUpdate.disconnected());
            }
          },
          onStompError: (_) {
            if (!controller.isClosed) {
              controller.add(const OrderTrackingRealtimeUpdate.disconnected());
            }
          },
        ),
      );
      _client = client;
      client.activate();
    } catch (error, stackTrace) {
      if (!controller.isClosed) {
        controller.add(const OrderTrackingRealtimeUpdate.disconnected());
        controller.addError(error, stackTrace);
      }
    }
  }

  Future<String> _accessToken() async {
    final tokens = await _tokenStore.read();
    if (tokens == null) {
      final refreshed = await _sessionRecovery.refreshNow();
      return refreshed.accessToken;
    }
    if (JwtClaimsDecoder.isExpiringSoon(tokens.accessToken)) {
      final refreshed = await _sessionRecovery.refreshNow();
      return refreshed.accessToken;
    }
    return tokens.accessToken;
  }

  Future<void> _disconnect() async {
    _unsubscribe?.call();
    _unsubscribe = null;
    final client = _client;
    _client = null;
    if (client != null) {
      client.deactivate();
    }
  }

  String _webSocketUrl() {
    final uri = Uri.parse(_baseUrl);
    final scheme = uri.scheme == 'https' ? 'wss' : 'ws';
    return uri.replace(scheme: scheme, path: '/api/v1/ws').toString();
  }

  OrderTrackingPoint? _decodePoint(String? raw) {
    if (raw == null || raw.trim().isEmpty) {
      return null;
    }
    final decoded = jsonDecode(raw);
    if (decoded is Map<String, dynamic>) {
      return OrderTrackingPoint.fromJson(decoded);
    }
    if (decoded is Map) {
      return OrderTrackingPoint.fromJson(
        decoded.map((key, value) => MapEntry(key.toString(), value)),
      );
    }
    return null;
  }
}
