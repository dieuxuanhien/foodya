import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../../../core/network/api_exception.dart';
import '../../domain/models/merchant_category_taxonomy.dart';
import '../../domain/models/merchant_menu_category.dart';
import '../../domain/models/merchant_menu_category_request.dart';
import '../../domain/models/merchant_menu_item.dart';
import '../../domain/models/merchant_menu_item_request.dart';

class MerchantCatalogRemoteDataSource {
  MerchantCatalogRemoteDataSource({
    required String baseUrl,
    required http.Client client,
  }) : _baseUrl = baseUrl,
       _client = client;

  final String _baseUrl;
  final http.Client _client;

  static const Duration _requestTimeout = Duration(seconds: 12);

  Future<List<MerchantCategoryTaxonomy>> listCategoryTaxonomies() async {
    final json = await _send(
      () => _client.get(
        Uri.parse('$_baseUrl/api/v1/restaurants/category-taxonomies'),
      ),
      'Category taxonomy request failed with status',
    );
    return _extractDataList(
      json,
    ).map(MerchantCategoryTaxonomy.fromJson).toList(growable: false);
  }

  Future<List<MerchantMenuCategory>> listCategories({
    required String accessToken,
    required String restaurantId,
  }) async {
    final uri = Uri.parse(
      '$_baseUrl/api/v1/merchant/restaurants/$restaurantId/menu-categories',
    ).replace(queryParameters: const {'page': '0', 'size': '200'});
    final json = await _send(
      () => _client.get(uri, headers: _headers(accessToken)),
      'Menu category request failed with status',
    );
    return _extractDataList(
      json,
    ).map(MerchantMenuCategory.fromJson).toList(growable: false);
  }

  Future<MerchantMenuCategory> createCategory({
    required String accessToken,
    required String restaurantId,
    required MerchantMenuCategoryRequest request,
  }) async {
    final json = await _send(
      () => _client.post(
        Uri.parse(
          '$_baseUrl/api/v1/merchant/restaurants/$restaurantId/menu-categories',
        ),
        headers: _headers(accessToken),
        body: jsonEncode(request.toJson()),
      ),
      'Menu category request failed with status',
    );
    return MerchantMenuCategory.fromJson(_extractDataMap(json));
  }

  Future<MerchantMenuCategory> updateCategory({
    required String accessToken,
    required String categoryId,
    required MerchantMenuCategoryRequest request,
  }) async {
    final json = await _send(
      () => _client.patch(
        Uri.parse('$_baseUrl/api/v1/merchant/menu-categories/$categoryId'),
        headers: _headers(accessToken),
        body: jsonEncode(request.toJson()),
      ),
      'Menu category request failed with status',
    );
    return MerchantMenuCategory.fromJson(_extractDataMap(json));
  }

  Future<void> deleteCategory({
    required String accessToken,
    required String categoryId,
  }) async {
    await _send(
      () => _client.delete(
        Uri.parse('$_baseUrl/api/v1/merchant/menu-categories/$categoryId'),
        headers: _headers(accessToken),
      ),
      'Menu category request failed with status',
    );
  }

  Future<List<MerchantMenuItem>> listMenuItems({
    required String accessToken,
    required String restaurantId,
  }) async {
    final uri = Uri.parse(
      '$_baseUrl/api/v1/merchant/restaurants/$restaurantId/menu-items',
    ).replace(queryParameters: const {'page': '0', 'size': '200'});
    final json = await _send(
      () => _client.get(uri, headers: _headers(accessToken)),
      'Menu item request failed with status',
    );
    return _extractDataList(
      json,
    ).map(MerchantMenuItem.fromJson).toList(growable: false);
  }

  Future<MerchantMenuItem> createMenuItem({
    required String accessToken,
    required String restaurantId,
    required MerchantMenuItemRequest request,
  }) async {
    final json = await _send(
      () => _client.post(
        Uri.parse(
          '$_baseUrl/api/v1/merchant/restaurants/$restaurantId/menu-items',
        ),
        headers: _headers(accessToken),
        body: jsonEncode(request.toJson()),
      ),
      'Menu item request failed with status',
    );
    return MerchantMenuItem.fromJson(_extractDataMap(json));
  }

  Future<MerchantMenuItem> updateMenuItem({
    required String accessToken,
    required String menuItemId,
    required MerchantMenuItemRequest request,
  }) async {
    final json = await _send(
      () => _client.patch(
        Uri.parse('$_baseUrl/api/v1/merchant/menu-items/$menuItemId'),
        headers: _headers(accessToken),
        body: jsonEncode(request.toJson()),
      ),
      'Menu item request failed with status',
    );
    return MerchantMenuItem.fromJson(_extractDataMap(json));
  }

  Future<MerchantMenuItem> updateMenuItemAvailability({
    required String accessToken,
    required String menuItemId,
    required bool isAvailable,
  }) async {
    final json = await _send(
      () => _client.patch(
        Uri.parse(
          '$_baseUrl/api/v1/merchant/menu-items/$menuItemId/availability',
        ),
        headers: _headers(accessToken),
        body: jsonEncode({'isAvailable': isAvailable}),
      ),
      'Menu item availability request failed with status',
    );
    return MerchantMenuItem.fromJson(_extractDataMap(json));
  }

  Future<void> deleteMenuItem({
    required String accessToken,
    required String menuItemId,
  }) async {
    await _send(
      () => _client.delete(
        Uri.parse('$_baseUrl/api/v1/merchant/menu-items/$menuItemId'),
        headers: _headers(accessToken),
      ),
      'Menu item request failed with status',
    );
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
      message: 'Malformed response payload from merchant catalog endpoint.',
    );
  }

  List<Map<String, dynamic>> _extractDataList(Map<String, dynamic> json) {
    final data = json['data'];
    if (data is! List) {
      throw const ApiException(
        statusCode: 500,
        code: 'MALFORMED_RESPONSE',
        message:
            'Malformed list response payload from merchant catalog endpoint.',
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
