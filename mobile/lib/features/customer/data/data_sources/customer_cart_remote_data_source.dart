import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../../../core/network/api_exception.dart';
import '../../domain/models/active_cart.dart';

class CustomerCartRemoteDataSource {
  CustomerCartRemoteDataSource({
    required String baseUrl,
    required http.Client client,
  }) : _baseUrl = baseUrl,
       _client = client;

  final String _baseUrl;
  final http.Client _client;

  static const Duration _requestTimeout = Duration(seconds: 12);

  Future<ActiveCart> getActiveCart({required String accessToken}) async {
    final json = await _get(
      '/api/v1/customer/carts/active',
      headers: _authHeaders(accessToken),
    );
    return ActiveCart.fromJson(_extractDataMap(json));
  }

  Future<ActiveCart> addItem({
    required String accessToken,
    required String menuItemId,
    required int quantity,
    String? note,
  }) async {
    final json = await _post(
      '/api/v1/customer/carts/active/items',
      headers: _authHeaders(accessToken),
      body: {
        'menuItemId': menuItemId,
        'quantity': quantity,
        if (note != null && note.trim().isNotEmpty) 'note': note.trim(),
      },
    );
    return ActiveCart.fromJson(_extractDataMap(json));
  }

  Future<ActiveCart> updateItem({
    required String accessToken,
    required String menuItemId,
    required int quantity,
    String? note,
  }) async {
    final json = await _patch(
      '/api/v1/customer/carts/active/items/$menuItemId',
      headers: _authHeaders(accessToken),
      body: {
        'quantity': quantity,
        if (note != null && note.trim().isNotEmpty) 'note': note.trim(),
      },
    );
    return ActiveCart.fromJson(_extractDataMap(json));
  }

  Future<ActiveCart> removeItem({
    required String accessToken,
    required String menuItemId,
  }) async {
    final json = await _delete(
      '/api/v1/customer/carts/active/items/$menuItemId',
      headers: _authHeaders(accessToken),
    );
    return ActiveCart.fromJson(_extractDataMap(json));
  }

  Future<ActiveCart> clearCart({required String accessToken}) async {
    final json = await _delete(
      '/api/v1/customer/carts/active/items',
      headers: _authHeaders(accessToken),
    );
    return ActiveCart.fromJson(_extractDataMap(json));
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
      'Cart request failed with status',
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
      'Cart request failed with status',
    );
  }

  Future<Map<String, dynamic>> _patch(
    String path, {
    Map<String, String>? headers,
    Map<String, dynamic>? body,
  }) async {
    return _send(
      () => _client.patch(
        Uri.parse('$_baseUrl$path'),
        headers: {'Content-Type': 'application/json', ...?headers},
        body: body == null ? null : jsonEncode(body),
      ),
      'Cart request failed with status',
    );
  }

  Future<Map<String, dynamic>> _delete(
    String path, {
    Map<String, String>? headers,
  }) async {
    return _send(
      () => _client.delete(
        Uri.parse('$_baseUrl$path'),
        headers: {'Content-Type': 'application/json', ...?headers},
      ),
      'Cart request failed with status',
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
      message: 'Malformed response payload from cart endpoint.',
    );
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
