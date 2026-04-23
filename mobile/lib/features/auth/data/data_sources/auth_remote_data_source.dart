import 'dart:convert';
import 'dart:async';

import 'package:http/http.dart' as http;

import '../../../../core/auth/auth_tokens.dart';
import '../../../../core/network/api_exception.dart';
import '../../domain/models/login_request.dart';
import '../../domain/models/register_request.dart';

class AuthRemoteDataSource {
  AuthRemoteDataSource({required String baseUrl, required http.Client client})
    : _baseUrl = baseUrl,
      _client = client;

  final String _baseUrl;
  final http.Client _client;

  Future<AuthTokens> register(RegisterRequest request) async {
    final data = await _postExpectData(
      '/api/v1/auth/register',
      body: request.toJson(),
    );
    return AuthTokens.fromJson(data);
  }

  Future<AuthTokens> login(LoginRequest request) async {
    final data = await _postExpectData(
      '/api/v1/auth/login',
      body: request.toJson(),
    );
    return AuthTokens.fromJson(data);
  }

  Future<AuthTokens> refresh(String refreshToken) async {
    final data = await _postExpectData(
      '/api/v1/auth/refresh',
      body: {'refreshToken': refreshToken},
    );
    return AuthTokens.fromJson(data);
  }

  Future<void> logoutAll(String accessToken) async {
    await _post(
      '/api/v1/auth/logout-all',
      headers: {'Authorization': 'Bearer $accessToken'},
    );
  }

  Future<Map<String, dynamic>> _postExpectData(
    String path, {
    Map<String, dynamic>? body,
    Map<String, String>? headers,
  }) async {
    final json = await _post(path, body: body, headers: headers);
    final data = json['data'];

    if (data is Map<String, dynamic>) {
      return data;
    }

    if (data is Map) {
      return data.map((key, value) => MapEntry(key.toString(), value));
    }

    throw ApiException(
      statusCode: 500,
      message: 'Malformed response payload from auth endpoint.',
      code: 'MALFORMED_RESPONSE',
      details: json,
    );
  }

  static const Duration _requestTimeout = Duration(seconds: 12);

  Future<Map<String, dynamic>> _post(
    String path, {
    Map<String, dynamic>? body,
    Map<String, String>? headers,
  }) async {
    late final http.Response response;
    try {
      response = await _client
          .post(
            Uri.parse('$_baseUrl$path'),
            headers: {'Content-Type': 'application/json', ...?headers},
            body: body == null ? null : jsonEncode(body),
          )
          .timeout(_requestTimeout);
    } on TimeoutException catch (error) {
      throw ApiException(
        statusCode: 0,
        code: 'NETWORK_TIMEOUT',
        message: 'Auth request timed out. Check API base URL and network.',
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

    final json = _tryDecodeMap(response.body);
    if (response.statusCode >= 200 && response.statusCode < 300) {
      return json ?? const {};
    }

    throw ApiException(
      statusCode: response.statusCode,
      code: json?['code']?.toString(),
      message:
          json?['message']?.toString() ??
          'Auth request failed with status ${response.statusCode}.',
      details: json?['details'],
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

      return null;
    } catch (_) {
      return null;
    }
  }
}
