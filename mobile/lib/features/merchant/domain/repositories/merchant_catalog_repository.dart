import '../models/merchant_category_taxonomy.dart';
import '../models/merchant_menu_category.dart';
import '../models/merchant_menu_category_request.dart';
import '../models/merchant_menu_item.dart';
import '../models/merchant_menu_item_request.dart';

abstract class MerchantCatalogRepository {
  Future<List<MerchantCategoryTaxonomy>> listCategoryTaxonomies();

  Future<List<MerchantMenuCategory>> listCategories(String restaurantId);

  Future<MerchantMenuCategory> createCategory({
    required String restaurantId,
    required MerchantMenuCategoryRequest request,
  });

  Future<MerchantMenuCategory> updateCategory({
    required String categoryId,
    required MerchantMenuCategoryRequest request,
  });

  Future<void> deleteCategory(String categoryId);

  Future<List<MerchantMenuItem>> listMenuItems(String restaurantId);

  Future<MerchantMenuItem> createMenuItem({
    required String restaurantId,
    required MerchantMenuItemRequest request,
  });

  Future<MerchantMenuItem> updateMenuItem({
    required String menuItemId,
    required MerchantMenuItemRequest request,
  });

  Future<MerchantMenuItem> updateMenuItemAvailability({
    required String menuItemId,
    required bool isAvailable,
  });

  Future<void> deleteMenuItem(String menuItemId);
}
