import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../network/api_exception.dart';
import 'location_address.dart';

class LocationAddressRemoteDataSource {
  LocationAddressRemoteDataSource({
    required String baseUrl,
    required http.Client client,
  }) : _baseUrl = baseUrl,
       _client = client;

  final String _baseUrl;
  final http.Client _client;

  static const Duration _requestTimeout = Duration(seconds: 12);

  Future<LocationAddress> resolveAddress({
    required double lat,
    required double lng,
    required String accessToken,
  }) async {
    final json = await _get(
      '/api/v1/me/location-address',
      headers: {'Authorization': 'Bearer $accessToken'},
      queryParameters: {'lat': lat.toString(), 'lng': lng.toString()},
    );

    final data = _extractDataMap(json);
    return LocationAddress.fromJson(data);
  }

  Future<Map<String, dynamic>> _get(
    String path, {
    Map<String, String>? headers,
    Map<String, String?>? queryParameters,
  }) async {
    late final http.Response response;
    try {
      final uri = Uri.parse(
        '$_baseUrl$path',
      ).replace(queryParameters: _sanitizeQuery(queryParameters));
      response = await _client
          .get(uri, headers: {'Content-Type': 'application/json', ...?headers})
          .timeout(_requestTimeout);
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
          'Address lookup failed with status ${response.statusCode}.',
      details: body?['details'],
    );
  }

  Map<String, String> _sanitizeQuery(Map<String, String?>? query) {
    if (query == null || query.isEmpty) {
      return const {};
    }

    final output = <String, String>{};
    for (final entry in query.entries) {
      final value = entry.value?.trim();
      if (value == null || value.isEmpty) {
        continue;
      }
      output[entry.key] = value;
    }

    return output;
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
      message: 'Malformed response payload from location endpoint.',
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
