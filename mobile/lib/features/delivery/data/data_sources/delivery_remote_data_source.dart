import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../../../core/network/api_exception.dart';

class DeliveryRemoteDataSource {
  DeliveryRemoteDataSource({
    required String baseUrl,
    required http.Client client,
    this.requestTimeout = const Duration(seconds: 12),
  }) : _baseUrl = baseUrl,
       _client = client;

  final String _baseUrl;
  final http.Client _client;
  final Duration requestTimeout;

  Future<Map<String, dynamic>> getSession(String token) =>
      _dataMap('GET', '/api/v1/delivery/status', token);

  Future<Map<String, dynamic>> goOnline(
    String token, {
    double? lat,
    double? lng,
  }) => _dataMap(
    'POST',
    '/api/v1/delivery/status/online',
    token,
    body: lat == null || lng == null ? null : {'lat': lat, 'lng': lng},
  );

  Future<Map<String, dynamic>> goOffline(String token) =>
      _dataMap('POST', '/api/v1/delivery/status/offline', token);

  Future<Map<String, dynamic>> updateLocation(
    String token,
    double lat,
    double lng,
  ) => _dataMap(
    'PUT',
    '/api/v1/delivery/status/location',
    token,
    body: {'lat': lat, 'lng': lng},
  );

  Future<List<Map<String, dynamic>>> getAssignments(String token) =>
      _dataList('GET', '/api/v1/delivery/orders/assignments', token);

  Future<Map<String, dynamic>> acceptAssignment(String token, String orderId) =>
      _dataMap('POST', '/api/v1/delivery/orders/$orderId/accept', token);

  Future<Map<String, dynamic>> rejectAssignment(String token, String orderId) =>
      _dataMap('POST', '/api/v1/delivery/orders/$orderId/reject', token);

  Future<Map<String, dynamic>> updateOrderStatus(
    String token,
    String orderId,
    String status,
  ) => _dataMap(
    'PATCH',
    '/api/v1/delivery/orders/$orderId/status',
    token,
    body: {'status': status},
  );

  Future<Map<String, dynamic>> getActiveBatch(String token) =>
      _dataMap('GET', '/api/v1/delivery/batches/active', token);

  Future<Map<String, dynamic>> getBatch(String token, String batchId) =>
      _dataMap('GET', '/api/v1/delivery/batches/$batchId', token);

  Future<void> markPickup(String token, String batchId, String orderId) =>
      _send(
        'POST',
        '/api/v1/delivery/batches/$batchId/orders/$orderId/pickup',
        token,
      );

  Future<void> markDelivered(String token, String batchId, String orderId) =>
      _send(
        'POST',
        '/api/v1/delivery/batches/$batchId/orders/$orderId/delivery',
        token,
      );

  Future<void> markFailed(
    String token,
    String batchId,
    String orderId, {
    String? reason,
  }) {
    final query = <String, String>{};
    if (reason != null && reason.trim().isNotEmpty) {
      query['reason'] = reason.trim();
    }
    return _send(
      'POST',
      '/api/v1/delivery/batches/$batchId/orders/$orderId/fail',
      token,
      query: query,
    );
  }

  Future<Map<String, dynamic>> getRoute(String token, String orderId) =>
      _dataMap('GET', '/api/v1/delivery/orders/$orderId/route', token);

  Future<Map<String, dynamic>> getEarnings(
    String token, {
    DateTime? from,
    DateTime? to,
  }) {
    final query = <String, String>{};
    if (from != null) query['from'] = from.toUtc().toIso8601String();
    if (to != null) query['to'] = to.toUtc().toIso8601String();
    return _dataMap('GET', '/api/v1/delivery/earnings', token, query: query);
  }

  Future<Map<String, dynamic>> _dataMap(
    String method,
    String path,
    String token, {
    Map<String, Object?>? body,
    Map<String, String> query = const {},
  }) async {
    final json = await _send(method, path, token, body: body, query: query);
    final data = json['data'];
    if (data is Map<String, dynamic>) return data;
    if (data is Map) {
      return data.map((key, value) => MapEntry(key.toString(), value));
    }
    throw const ApiException(
      statusCode: 500,
      code: 'MALFORMED_RESPONSE',
      message: 'The Delivery endpoint returned an invalid object payload.',
    );
  }

  Future<List<Map<String, dynamic>>> _dataList(
    String method,
    String path,
    String token,
  ) async {
    final json = await _send(method, path, token);
    final data = json['data'];
    if (data is! List) {
      throw const ApiException(
        statusCode: 500,
        code: 'MALFORMED_RESPONSE',
        message: 'The Delivery endpoint returned an invalid list payload.',
      );
    }
    return data
        .whereType<Map>()
        .map(
          (item) => item.map((key, value) => MapEntry(key.toString(), value)),
        )
        .toList(growable: false);
  }

  Future<Map<String, dynamic>> _send(
    String method,
    String path,
    String token, {
    Map<String, Object?>? body,
    Map<String, String> query = const {},
  }) async {
    final base = Uri.parse('$_baseUrl$path');
    final uri = query.isEmpty ? base : base.replace(queryParameters: query);
    final headers = {
      'Authorization': 'Bearer $token',
      'Content-Type': 'application/json',
    };
    try {
      final response = switch (method) {
        'GET' => await _client
            .get(uri, headers: headers)
            .timeout(requestTimeout),
        'POST' => await _client
            .post(
              uri,
              headers: headers,
              body: body == null ? null : jsonEncode(body),
            )
            .timeout(requestTimeout),
        'PUT' => await _client
            .put(uri, headers: headers, body: jsonEncode(body))
            .timeout(requestTimeout),
        'PATCH' => await _client
            .patch(uri, headers: headers, body: jsonEncode(body))
            .timeout(requestTimeout),
        _ => throw StateError('Unsupported HTTP method $method'),
      };
      final decoded = _decodeMap(response.body);
      if (response.statusCode >= 200 && response.statusCode < 300) {
        return decoded ?? const {};
      }
      throw ApiException(
        statusCode: response.statusCode,
        code: decoded?['code']?.toString(),
        message:
            decoded?['message']?.toString() ??
            'Delivery request failed with status ${response.statusCode}.',
        details: decoded?['details'],
      );
    } on ApiException {
      rethrow;
    } on TimeoutException catch (error) {
      throw ApiException(
        statusCode: 0,
        code: 'NETWORK_TIMEOUT',
        message: 'Request timed out. Check your connection and try again.',
        details: error.toString(),
      );
    } catch (error) {
      throw ApiException(
        statusCode: 0,
        code: 'NETWORK_ERROR',
        message: 'Cannot reach Foodya backend. Check API base URL and network.',
        details: error.toString(),
      );
    }
  }

  Map<String, dynamic>? _decodeMap(String raw) {
    if (raw.trim().isEmpty) return null;
    try {
      final decoded = jsonDecode(raw);
      if (decoded is Map<String, dynamic>) return decoded;
      if (decoded is Map) {
        return decoded.map((key, value) => MapEntry(key.toString(), value));
      }
    } catch (_) {
      return null;
    }
    return null;
  }
}
