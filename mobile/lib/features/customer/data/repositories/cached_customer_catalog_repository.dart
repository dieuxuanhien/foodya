import '../../../../core/cache/ttl_cache.dart';
import '../../domain/models/category_taxonomy.dart';
import '../../domain/models/order_review.dart';
import '../../domain/models/paged_result.dart';
import '../../domain/models/restaurant_detail.dart';
import '../../domain/models/restaurant_menu_item.dart';
import '../../domain/models/restaurant_search_item.dart';
import '../../domain/repositories/customer_catalog_repository.dart';

/// Wraps a [CustomerCatalogRepository] with short-lived in-memory caches for
/// the handful of calls that repeat across screen switches: category
/// taxonomies (near-static reference data shown on three screens), restaurant
/// detail (revisited as users bounce between Browse and Detail), and nearby
/// restaurants (reloaded every time the customer returns to Home).
///
/// Search and menu-item listing are intentionally left uncached — too many
/// filter/pagination permutations for a useful hit rate, and freshness matters
/// more than speed for active search results.
class CachedCustomerCatalogRepository implements CustomerCatalogRepository {
  CachedCustomerCatalogRepository({required CustomerCatalogRepository delegate})
    : _delegate = delegate;

  static const _taxonomiesTtl = Duration(minutes: 30);
  static const _restaurantDetailTtl = Duration(minutes: 5);
  static const _nearbyTtl = Duration(minutes: 2);

  final CustomerCatalogRepository _delegate;
  final TtlCache<String, List<CategoryTaxonomy>> _taxonomiesCache = TtlCache();
  final TtlCache<String, RestaurantDetail> _restaurantDetailCache = TtlCache();
  final TtlCache<String, PagedResult<RestaurantSearchItem>> _nearbyCache =
      TtlCache();

  @override
  Future<List<CategoryTaxonomy>> listCategoryTaxonomies() {
    return _taxonomiesCache.get(
      'taxonomies',
      loader: _delegate.listCategoryTaxonomies,
      ttl: _taxonomiesTtl,
    );
  }

  @override
  Future<RestaurantDetail> getRestaurantDetail(
    String restaurantId, {
    bool forceRefresh = false,
  }) {
    return _restaurantDetailCache.get(
      restaurantId,
      loader: () => _delegate.getRestaurantDetail(restaurantId),
      ttl: _restaurantDetailTtl,
      forceRefresh: forceRefresh,
    );
  }

  @override
  Future<PagedResult<RestaurantSearchItem>> nearbyRestaurants({
    required double lat,
    required double lng,
    double radiusKm = 5.0,
    String? sort,
    int page = 0,
    int size = 10,
    bool forceRefresh = false,
  }) {
    final key =
        '${lat.toStringAsFixed(2)}:${lng.toStringAsFixed(2)}:$radiusKm:$sort:$page:$size';
    return _nearbyCache.get(
      key,
      loader:
          () => _delegate.nearbyRestaurants(
            lat: lat,
            lng: lng,
            radiusKm: radiusKm,
            sort: sort,
            page: page,
            size: size,
          ),
      ttl: _nearbyTtl,
      forceRefresh: forceRefresh,
    );
  }

  @override
  Future<PagedResult<RestaurantSearchItem>> searchRestaurants({
    String? keyword,
    String? cuisine,
    double? minRating,
    bool? openNow,
    String? sort,
    List<String> taxonomyCodes = const [],
    int page = 0,
    int size = 10,
  }) {
    return _delegate.searchRestaurants(
      keyword: keyword,
      cuisine: cuisine,
      minRating: minRating,
      openNow: openNow,
      sort: sort,
      taxonomyCodes: taxonomyCodes,
      page: page,
      size: size,
    );
  }

  @override
  Future<PagedResult<RestaurantMenuItem>> getRestaurantMenuItems(
    String restaurantId, {
    String? keyword,
    String? sort,
    List<String> taxonomyCodes = const [],
    int page = 0,
    int size = 20,
  }) {
    return _delegate.getRestaurantMenuItems(
      restaurantId,
      keyword: keyword,
      sort: sort,
      taxonomyCodes: taxonomyCodes,
      page: page,
      size: size,
    );
  }

  @override
  Future<List<OrderReview>> listRestaurantReviews(String restaurantId) {
    return _delegate.listRestaurantReviews(restaurantId);
  }
}
