import '../../domain/models/category_taxonomy.dart';
import '../../domain/models/order_review.dart';
import '../../domain/models/paged_result.dart';
import '../../domain/models/restaurant_detail.dart';
import '../../domain/models/restaurant_menu_item.dart';
import '../../domain/models/restaurant_search_item.dart';
import '../../domain/repositories/customer_catalog_repository.dart';
import '../data_sources/customer_catalog_remote_data_source.dart';

class HttpCustomerCatalogRepository implements CustomerCatalogRepository {
  HttpCustomerCatalogRepository({
    required CustomerCatalogRemoteDataSource remoteDataSource,
  }) : _remoteDataSource = remoteDataSource;

  final CustomerCatalogRemoteDataSource _remoteDataSource;

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
    return _remoteDataSource.searchRestaurants(
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
  Future<RestaurantDetail> getRestaurantDetail(
    String restaurantId, {
    bool forceRefresh = false,
  }) {
    return _remoteDataSource.getRestaurantDetail(restaurantId);
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
    return _remoteDataSource.getRestaurantMenuItems(
      restaurantId,
      keyword: keyword,
      sort: sort,
      taxonomyCodes: taxonomyCodes,
      page: page,
      size: size,
    );
  }

  @override
  Future<List<CategoryTaxonomy>> listCategoryTaxonomies() {
    return _remoteDataSource.listCategoryTaxonomies();
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
    return _remoteDataSource.nearbyRestaurants(
      lat: lat,
      lng: lng,
      radiusKm: radiusKm,
      sort: sort,
      page: page,
      size: size,
    );
  }

  @override
  Future<List<OrderReview>> listRestaurantReviews(String restaurantId) {
    return _remoteDataSource.listRestaurantReviews(restaurantId);
  }
}
