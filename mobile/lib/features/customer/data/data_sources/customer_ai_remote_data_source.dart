import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../../../core/network/api_exception.dart';
import '../../domain/models/ai_chat.dart';

class CustomerAiRemoteDataSource {
  CustomerAiRemoteDataSource({
    required String baseUrl,
    required http.Client client,
  }) : _baseUrl = baseUrl,
       _client = client;

  final String _baseUrl;
  final http.Client _client;

  static const Duration _requestTimeout = Duration(seconds: 45);

  Future<List<AiChatHistoryItem>> history({required String accessToken}) async {
    final json = await _get(
      '/api/v1/customer/ai/chats',
      headers: _authHeaders(accessToken),
    );
    return _extractDataList(
      json,
    ).map(AiChatHistoryItem.fromJson).toList(growable: false);
  }

  Future<AiChatResponse> createChat({
    required String accessToken,
    required String prompt,
    double? lat,
    double? lng,
  }) async {
    final json = await _post(
      '/api/v1/customer/ai/chats',
      headers: _authHeaders(accessToken),
      body: {
        'prompt': prompt,
        if (lat != null) 'lat': lat,
        if (lng != null) 'lng': lng,
      },
    );
    return AiChatResponse.fromJson(_extractDataMap(json));
  }

  Future<void> deleteConversation({required String accessToken}) async {
    await _delete(
      '/api/v1/customer/ai/chats',
      headers: _authHeaders(accessToken),
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
      'AI request failed with status',
    );
  }

  Future<Map<String, dynamic>> _post(
    String path, {
    Map<String, String>? headers,
    Map<String, dynamic>? body,
  }) {
    return _send(
      () => _client.post(
        Uri.parse('$_baseUrl$path'),
        headers: {'Content-Type': 'application/json', ...?headers},
        body: body == null ? null : jsonEncode(body),
      ),
      'AI request failed with status',
    );
  }

  Future<Map<String, dynamic>> _delete(
    String path, {
    Map<String, String>? headers,
  }) {
    return _send(
      () => _client.delete(
        Uri.parse('$_baseUrl$path'),
        headers: {'Content-Type': 'application/json', ...?headers},
      ),
      'AI request failed with status',
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
      message: 'Malformed response payload from AI endpoint.',
    );
  }

  List<Map<String, dynamic>> _extractDataList(Map<String, dynamic> json) {
    final data = json['data'];
    if (data is! List) {
      throw const ApiException(
        statusCode: 500,
        code: 'MALFORMED_RESPONSE',
        message: 'Malformed list response payload from AI endpoint.',
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
