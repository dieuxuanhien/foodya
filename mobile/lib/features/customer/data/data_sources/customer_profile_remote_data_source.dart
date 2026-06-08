import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:http_parser/http_parser.dart';
import 'package:image_picker/image_picker.dart';
import 'package:mime/mime.dart';

import '../../../../core/network/api_exception.dart';
import '../../domain/models/user_profile.dart';

class CustomerProfileRemoteDataSource {
  CustomerProfileRemoteDataSource({
    required String baseUrl,
    required http.Client client,
  }) : _baseUrl = baseUrl,
       _client = client;

  final String _baseUrl;
  final http.Client _client;

  static const Duration _requestTimeout = Duration(seconds: 12);
  static const Duration _uploadTimeout = Duration(seconds: 60);

  Future<UserProfile> me({required String accessToken}) async {
    final json = await _get('/api/v1/me', headers: _authHeaders(accessToken));
    return UserProfile.fromJson(_extractDataMap(json));
  }

  Future<UserProfile> updateProfile({
    required String accessToken,
    required String fullName,
    required String email,
    required String phoneNumber,
    String? avatarUrl,
  }) async {
    final json = await _patch(
      '/api/v1/me',
      headers: _authHeaders(accessToken),
      body: {
        'fullName': fullName,
        'email': email,
        'phoneNumber': phoneNumber,
        'avatarUrl': avatarUrl,
      },
    );
    return UserProfile.fromJson(_extractDataMap(json));
  }

  Future<void> changePassword({
    required String accessToken,
    required String currentPassword,
    required String newPassword,
    required String confirmPassword,
  }) async {
    await _put(
      '/api/v1/me/password',
      headers: _authHeaders(accessToken),
      body: {
        'currentPassword': currentPassword,
        'newPassword': newPassword,
        'confirmPassword': confirmPassword,
      },
    );
  }

  Future<UserProfile> uploadAvatar({
    required String accessToken,
    required XFile file,
  }) async {
    final bytes = await file.readAsBytes();
    final mimeType =
        file.mimeType ??
        lookupMimeType(file.name) ??
        'application/octet-stream';
    final multipart =
        http.MultipartRequest(
          'POST',
          Uri.parse('$_baseUrl/api/v1/me/avatar'),
        )
          ..headers['Authorization'] = 'Bearer $accessToken'
          ..files.add(
            http.MultipartFile.fromBytes(
              'file',
              bytes,
              filename: file.name,
              contentType: MediaType.parse(mimeType),
            ),
          );

    late final http.Response response;
    try {
      final streamed = await multipart.send().timeout(_uploadTimeout);
      response = await http.Response.fromStream(streamed);
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
      return UserProfile.fromJson(_extractDataMap(body ?? const {}));
    }
    throw ApiException(
      statusCode: response.statusCode,
      code: body?['code']?.toString(),
      message:
          body?['message']?.toString() ??
          'Avatar upload failed with status ${response.statusCode}.',
      details: body?['details'],
    );
  }

  Map<String, String> _authHeaders(String accessToken) {
    return {'Authorization': 'Bearer $accessToken'};
  }

  Future<Map<String, dynamic>> _get(
    String path, {
    Map<String, String>? headers,
  }) {
    return _send(
      () => _client.get(
        Uri.parse('$_baseUrl$path'),
        headers: {'Content-Type': 'application/json', ...?headers},
      ),
      'Profile request failed with status',
    );
  }

  Future<Map<String, dynamic>> _patch(
    String path, {
    Map<String, String>? headers,
    Map<String, dynamic>? body,
  }) {
    return _send(
      () => _client.patch(
        Uri.parse('$_baseUrl$path'),
        headers: {'Content-Type': 'application/json', ...?headers},
        body: body == null ? null : jsonEncode(body),
      ),
      'Profile request failed with status',
    );
  }

  Future<Map<String, dynamic>> _put(
    String path, {
    Map<String, String>? headers,
    Map<String, dynamic>? body,
  }) {
    return _send(
      () => _client.put(
        Uri.parse('$_baseUrl$path'),
        headers: {'Content-Type': 'application/json', ...?headers},
        body: body == null ? null : jsonEncode(body),
      ),
      'Profile request failed with status',
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
      message: 'Malformed response payload from profile endpoint.',
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
