import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../../../core/network/api_exception.dart';
import '../../domain/models/create_order_request.dart';
import '../../domain/models/order_cost_review.dart';
import '../../domain/models/order_created.dart';
import '../../domain/models/order_detail.dart';
import '../../domain/models/order_review.dart';
import '../../domain/models/order_summary.dart';
import '../../domain/models/order_tracking_point.dart';

class CustomerOrderRemoteDataSource {
  CustomerOrderRemoteDataSource({
    required String baseUrl,
    required http.Client client,
  }) : _baseUrl = baseUrl,
       _client = client;

  final String _baseUrl;
  final http.Client _client;

  static const Duration _requestTimeout = Duration(seconds: 12);

  Future<OrderCostReview> reviewOrderCost({
    required String accessToken,
    required CreateOrderRequest request,
  }) async {
    final json = await _post(
      '/api/v1/customer/orders/review',
      headers: _authHeaders(accessToken),
      body: request.toJson(),
    );
    return OrderCostReview.fromJson(_extractDataMap(json));
  }

  Future<OrderCreated> createOrder({
    required String accessToken,
    required CreateOrderRequest request,
    required String idempotencyKey,
  }) async {
    final json = await _post(
      '/api/v1/customer/orders',
      headers: {
        ..._authHeaders(accessToken),
        'Idempotency-Key': idempotencyKey,
      },
      body: request.toJson(),
    );
    return OrderCreated.fromJson(_extractDataMap(json));
  }

  Future<List<OrderSummary>> listOrders({required String accessToken}) async {
    final json = await _get(
      '/api/v1/customer/orders',
      headers: _authHeaders(accessToken),
    );
    return _extractDataList(
      json,
    ).map(OrderSummary.fromJson).toList(growable: false);
  }

  Future<OrderDetail> getOrderDetail({
    required String accessToken,
    required String orderId,
  }) async {
    final json = await _get(
      '/api/v1/customer/orders/$orderId',
      headers: _authHeaders(accessToken),
    );
    return OrderDetail.fromJson(_extractDataMap(json));
  }

  Future<OrderDetail> cancelOrder({
    required String accessToken,
    required String orderId,
    String? reason,
  }) async {
    final json = await _post(
      '/api/v1/customer/orders/$orderId/cancel',
      headers: _authHeaders(accessToken),
      body:
          reason == null || reason.trim().isEmpty
              ? null
              : {'reason': reason.trim()},
    );
    return OrderDetail.fromJson(_extractDataMap(json));
  }

  Future<List<OrderTrackingPoint>> trackingPoints({
    required String accessToken,
    required String orderId,
  }) async {
    final json = await _get(
      '/api/v1/customer/orders/$orderId/tracking',
      headers: _authHeaders(accessToken),
    );
    return _extractDataList(
      json,
    ).map(OrderTrackingPoint.fromJson).toList(growable: false);
  }

  Future<OrderReview> createReview({
    required String accessToken,
    required String orderId,
    required int stars,
    String? comment,
  }) async {
    final json = await _post(
      '/api/v1/customer/orders/$orderId/reviews',
      headers: _authHeaders(accessToken),
      body: {
        'stars': stars,
        if (comment != null && comment.trim().isNotEmpty)
          'comment': comment.trim(),
      },
    );
    return OrderReview.fromJson(_extractDataMap(json));
  }

  Map<String, String> _authHeaders(String accessToken) {
    return {'Authorization': 'Bearer $accessToken'};
  }

  Future<Map<String, dynamic>> _get(
    String path, {
    Map<String, String>? headers,
  }) async {
    return _send(
      () => _client.get(
        Uri.parse('$_baseUrl$path'),
        headers: {'Content-Type': 'application/json', ...?headers},
      ),
      'Order request failed with status',
    );
  }

  Future<Map<String, dynamic>> _post(
    String path, {
    Map<String, String>? headers,
    Map<String, dynamic>? body,
  }) async {
    return _send(
      () => _client.post(
        Uri.parse('$_baseUrl$path'),
        headers: {'Content-Type': 'application/json', ...?headers},
        body: body == null ? null : jsonEncode(body),
      ),
      'Order request failed with status',
    );
  }

  Future<Map<String, dynamic>> _send(
    Future<http.Response> Function() request,
    String fallbackMessage,
  ) async {
    late final http.Response response;
    try {
      response = await request().timeout(_requestTimeout);
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

    final body = _tryDecodeMap(response.body);
    if (response.statusCode >= 200 && response.statusCode < 300) {
      return body ?? const {};
    }

    throw ApiException(
      statusCode: response.statusCode,
      code: body?['code']?.toString(),
      message:
          body?['message']?.toString() ??
          '$fallbackMessage ${response.statusCode}.',
      details: body?['details'],
    );
  }

  Map<String, dynamic> _extractDataMap(Map<String, dynamic> json) {
    final data = json['data'];
    if (data is Map<String, dynamic>) {
      return data;
    }
    if (data is Map) {
      return data.map((key, value) => MapEntry(key.toString(), value));
    }

    throw const ApiException(
      statusCode: 500,
      code: 'MALFORMED_RESPONSE',
      message: 'Malformed response payload from order endpoint.',
    );
  }

  List<Map<String, dynamic>> _extractDataList(Map<String, dynamic> json) {
    final data = json['data'];
    if (data is! List) {
      throw const ApiException(
        statusCode: 500,
        code: 'MALFORMED_RESPONSE',
        message: 'Malformed list response payload from order endpoint.',
      );
    }

    return data
        .whereType<Map>()
        .map(
          (item) => item.map((key, value) => MapEntry(key.toString(), value)),
        )
        .toList(growable: false);
  }

  Map<String, dynamic>? _tryDecodeMap(String raw) {
    if (raw.trim().isEmpty) {
      return null;
    }

    try {
      final decoded = jsonDecode(raw);
      if (decoded is Map<String, dynamic>) {
        return decoded;
      }
      if (decoded is Map) {
        return decoded.map((key, value) => MapEntry(key.toString(), value));
      }
    } catch (_) {
      return null;
    }

    return null;
  }
}
