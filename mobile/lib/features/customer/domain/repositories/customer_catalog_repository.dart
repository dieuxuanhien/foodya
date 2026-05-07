import '../models/category_taxonomy.dart';
import '../models/paged_result.dart';
import '../models/restaurant_detail.dart';
import '../models/restaurant_menu_item.dart';
import '../models/restaurant_search_item.dart';

abstract class CustomerCatalogRepository {
  Future<PagedResult<RestaurantSearchItem>> searchRestaurants({
    String? keyword,
    String? cuisine,
    double? minRating,
    bool? openNow,
    String? sort,
    List<String> taxonomyCodes = const [],
    int page = 0,
    int size = 10,
  });

  Future<RestaurantDetail> getRestaurantDetail(String restaurantId);

  Future<PagedResult<RestaurantMenuItem>> getRestaurantMenuItems(
    String restaurantId, {
    String? keyword,
    String? sort,
    List<String> taxonomyCodes = const [],
    int page = 0,
    int size = 20,
  });

  Future<List<CategoryTaxonomy>> listCategoryTaxonomies();
}
