import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../../../core/network/api_exception.dart';
import '../../domain/models/merchant_restaurant.dart';
import '../../domain/models/merchant_restaurant_request.dart';

class MerchantRestaurantRemoteDataSource {
  MerchantRestaurantRemoteDataSource({
    required String baseUrl,
    required http.Client client,
  }) : _baseUrl = baseUrl,
       _client = client;

  final String _baseUrl;
  final http.Client _client;

  static const Duration _requestTimeout = Duration(seconds: 12);

  Future<List<MerchantRestaurant>> listRestaurants({
    required String accessToken,
  }) async {
    final json = await _send(
      () => _client.get(
        Uri.parse('$_baseUrl/api/v1/merchant/restaurants'),
        headers: _headers(accessToken),
      ),
      'Restaurant list request failed with status',
    );
    return _extractDataList(
      json,
    ).map(MerchantRestaurant.fromJson).toList(growable: false);
  }

  Future<MerchantRestaurant> createRestaurant({
    required String accessToken,
    required MerchantRestaurantRequest request,
  }) async {
    final json = await _send(
      () => _client.post(
        Uri.parse('$_baseUrl/api/v1/merchant/restaurants'),
        headers: _headers(accessToken),
        body: jsonEncode(request.toJson()),
      ),
      'Restaurant request failed with status',
    );
    return MerchantRestaurant.fromJson(_extractDataMap(json));
  }

  Future<MerchantRestaurant> updateRestaurant({
    required String accessToken,
    required String restaurantId,
    required MerchantRestaurantRequest request,
  }) async {
    final json = await _send(
      () => _client.patch(
        Uri.parse('$_baseUrl/api/v1/merchant/restaurants/$restaurantId'),
        headers: _headers(accessToken),
        body: jsonEncode(request.toJson()),
      ),
      'Restaurant request failed with status',
    );
    return MerchantRestaurant.fromJson(_extractDataMap(json));
  }

  Map<String, String> _headers(String accessToken) {
    return {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer $accessToken',
    };
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
      message: 'Malformed response payload from merchant restaurant endpoint.',
    );
  }

  List<Map<String, dynamic>> _extractDataList(Map<String, dynamic> json) {
    final data = json['data'];
    if (data is List) {
      return data
          .map((item) {
            if (item is Map<String, dynamic>) {
              return item;
            }
            if (item is Map) {
              return item.map((key, value) => MapEntry(key.toString(), value));
            }
            throw const ApiException(
              statusCode: 500,
              code: 'MALFORMED_RESPONSE',
              message:
                  'Malformed response payload from merchant restaurant endpoint.',
            );
          })
          .toList(growable: false);
    }
    throw const ApiException(
      statusCode: 500,
      code: 'MALFORMED_RESPONSE',
      message: 'Malformed response payload from merchant restaurant endpoint.',
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
