import 'package:image_picker/image_picker.dart';

import '../../../../core/auth/auth_session_recovery.dart';
import '../../domain/models/merchant_category_taxonomy.dart';
import '../../domain/models/merchant_menu_category.dart';
import '../../domain/models/merchant_menu_category_request.dart';
import '../../domain/models/merchant_menu_item.dart';
import '../../domain/models/merchant_menu_item_request.dart';
import '../../domain/repositories/merchant_catalog_repository.dart';
import '../data_sources/merchant_catalog_remote_data_source.dart';

class HttpMerchantCatalogRepository implements MerchantCatalogRepository {
  HttpMerchantCatalogRepository({
    required MerchantCatalogRemoteDataSource remoteDataSource,
    required AuthSessionRecovery sessionRecovery,
  }) : _remoteDataSource = remoteDataSource,
       _sessionRecovery = sessionRecovery;

  final MerchantCatalogRemoteDataSource _remoteDataSource;
  final AuthSessionRecovery _sessionRecovery;

  @override
  Future<List<MerchantCategoryTaxonomy>> listCategoryTaxonomies() {
    return _remoteDataSource.listCategoryTaxonomies();
  }

  @override
  Future<List<MerchantMenuCategory>> listCategories(String restaurantId) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.listCategories(
        accessToken: accessToken,
        restaurantId: restaurantId,
      );
    });
  }

  @override
  Future<MerchantMenuCategory> createCategory({
    required String restaurantId,
    required MerchantMenuCategoryRequest request,
  }) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.createCategory(
        accessToken: accessToken,
        restaurantId: restaurantId,
        request: request,
      );
    });
  }

  @override
  Future<MerchantMenuCategory> updateCategory({
    required String categoryId,
    required MerchantMenuCategoryRequest request,
  }) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.updateCategory(
        accessToken: accessToken,
        categoryId: categoryId,
        request: request,
      );
    });
  }

  @override
  Future<void> deleteCategory(String categoryId) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.deleteCategory(
        accessToken: accessToken,
        categoryId: categoryId,
      );
    });
  }

  @override
  Future<List<MerchantMenuItem>> listMenuItems(String restaurantId) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.listMenuItems(
        accessToken: accessToken,
        restaurantId: restaurantId,
      );
    });
  }

  @override
  Future<MerchantMenuItem> createMenuItem({
    required String restaurantId,
    required MerchantMenuItemRequest request,
    required XFile imageFile,
  }) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.createMenuItem(
        accessToken: accessToken,
        restaurantId: restaurantId,
        request: request,
        imageFile: imageFile,
      );
    });
  }

  @override
  Future<MerchantMenuItem> updateMenuItem({
    required String menuItemId,
    required MerchantMenuItemRequest request,
  }) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.updateMenuItem(
        accessToken: accessToken,
        menuItemId: menuItemId,
        request: request,
      );
    });
  }

  @override
  Future<MerchantMenuItem> updateMenuItemAvailability({
    required String menuItemId,
    required bool isAvailable,
  }) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.updateMenuItemAvailability(
        accessToken: accessToken,
        menuItemId: menuItemId,
        isAvailable: isAvailable,
      );
    });
  }

  @override
  Future<void> deleteMenuItem(String menuItemId) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.deleteMenuItem(
        accessToken: accessToken,
        menuItemId: menuItemId,
      );
    });
  }
}
