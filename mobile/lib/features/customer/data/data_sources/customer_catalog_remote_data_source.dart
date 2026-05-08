import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../../../core/network/api_exception.dart';
import '../../domain/models/category_taxonomy.dart';
import '../../domain/models/paged_result.dart';
import '../../domain/models/restaurant_detail.dart';
import '../../domain/models/restaurant_menu_item.dart';
import '../../domain/models/restaurant_search_item.dart';

class CustomerCatalogRemoteDataSource {
  CustomerCatalogRemoteDataSource({
    required String baseUrl,
    required http.Client client,
  }) : _baseUrl = baseUrl,
       _client = client;

  final String _baseUrl;
  final http.Client _client;

  static const Duration _requestTimeout = Duration(seconds: 12);

  Future<PagedResult<RestaurantSearchItem>> searchRestaurants({
    String? keyword,
    String? cuisine,
    double? minRating,
    bool? openNow,
    String? sort,
    List<String> taxonomyCodes = const [],
    int page = 0,
    int size = 10,
  }) async {
    final response = await _get(
      '/api/v1/restaurants',
      queryParameters: {
        'q': keyword,
        'cuisine': cuisine,
        'minRating': minRating?.toString(),
        'openNow': openNow?.toString(),
        'sort': sort,
        'taxonomyCodes': taxonomyCodes.isEmpty ? null : taxonomyCodes.join(','),
        'page': page.toString(),
        'size': size.toString(),
      },
    );

    return _mapPagedData(
      response,
      (entry) => RestaurantSearchItem.fromJson(entry),
    );
  }

  Future<RestaurantDetail> getRestaurantDetail(String restaurantId) async {
    final response = await _get('/api/v1/restaurants/$restaurantId');
    final data = _extractDataMap(response);
    return RestaurantDetail.fromJson(data);
  }

  Future<PagedResult<RestaurantMenuItem>> getRestaurantMenuItems(
    String restaurantId, {
    String? keyword,
    String? sort,
    List<String> taxonomyCodes = const [],
    int page = 0,
    int size = 20,
  }) async {
    final response = await _get(
      '/api/v1/restaurants/$restaurantId/menu-items',
      queryParameters: {
        'q': keyword,
        'sort': sort,
        'taxonomyCodes': taxonomyCodes.isEmpty ? null : taxonomyCodes.join(','),
        'page': page.toString(),
        'size': size.toString(),
      },
    );

    return _mapPagedData(
      response,
      (entry) => RestaurantMenuItem.fromJson(entry),
    );
  }

  Future<PagedResult<RestaurantSearchItem>> nearbyRestaurants({
    required double lat,
    required double lng,
    double radiusKm = 5.0,
    String? sort,
    int page = 0,
    int size = 10,
  }) async {
    final response = await _get(
      '/api/v1/restaurants/nearby',
      queryParameters: {
        'lat': lat.toString(),
        'lng': lng.toString(),
        'radiusKm': radiusKm.toString(),
        'sort': sort,
        'page': page.toString(),
        'size': size.toString(),
      },
    );

    return _mapPagedData(
      response,
      (entry) => RestaurantSearchItem.fromJson(entry),
    );
  }

  Future<List<CategoryTaxonomy>> listCategoryTaxonomies() async {
    final response = await _get('/api/v1/restaurants/category-taxonomies');
    final data = _extractDataList(response);
    return data.map(CategoryTaxonomy.fromJson).toList(growable: false);
  }

  Future<Map<String, dynamic>> _get(
    String path, {
    Map<String, String?>? queryParameters,
  }) async {
    late final http.Response response;
    try {
      final uri = Uri.parse(
        '$_baseUrl$path',
      ).replace(queryParameters: _sanitizeQuery(queryParameters));
      response = await _client.get(uri).timeout(_requestTimeout);
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
          'Catalog request failed with status ${response.statusCode}.',
      details: body?['details'],
    );
  }

  PagedResult<T> _mapPagedData<T>(
    Map<String, dynamic> json,
    T Function(Map<String, dynamic>) mapper,
  ) {
    final items = _extractDataList(json).map(mapper).toList(growable: false);
    final meta = _extractMetaMap(json);

    return PagedResult<T>(
      items: items,
      page: _toInt(meta['page']),
      size: _toInt(meta['size']),
      totalElements: _toInt(meta['totalElements']),
      totalPages: _toInt(meta['totalPages']),
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
      message: 'Malformed response payload from catalog endpoint.',
    );
  }

  List<Map<String, dynamic>> _extractDataList(Map<String, dynamic> json) {
    final data = json['data'];
    if (data is! List) {
      throw const ApiException(
        statusCode: 500,
        code: 'MALFORMED_RESPONSE',
        message: 'Malformed list response payload from catalog endpoint.',
      );
    }

    return data
        .whereType<Map>()
        .map(
          (item) => item.map((key, value) => MapEntry(key.toString(), value)),
        )
        .toList(growable: false);
  }

  Map<String, dynamic> _extractMetaMap(Map<String, dynamic> json) {
    final meta = json['meta'];
    if (meta is Map<String, dynamic>) {
      return meta;
    }

    if (meta is Map) {
      return meta.map((key, value) => MapEntry(key.toString(), value));
    }

    return const {};
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

  int _toInt(Object? value) {
    if (value is int) {
      return value;
    }

    return int.tryParse(value?.toString() ?? '') ?? 0;
  }
}
