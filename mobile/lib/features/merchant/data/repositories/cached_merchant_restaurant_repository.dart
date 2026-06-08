import 'package:image_picker/image_picker.dart';

import '../../../../core/cache/ttl_cache.dart';
import '../../domain/models/merchant_restaurant.dart';
import '../../domain/models/merchant_restaurant_request.dart';
import '../../domain/repositories/merchant_restaurant_repository.dart';

/// Wraps a [MerchantRestaurantRepository] with a short-lived cache for
/// [listRestaurants], which the merchant dashboard reloads on every visit
/// (`merchant_home_cubit.dart`). Mutating calls bypass the cache and
/// invalidate it so the next list reflects the change immediately.
///
/// This data is per-account — call [clearCache] on sign-out (see
/// `SessionCubit.signOut`) so a second account on the same device never sees
/// a previous merchant's cached restaurants.
class CachedMerchantRestaurantRepository implements MerchantRestaurantRepository {
  CachedMerchantRestaurantRepository({
    required MerchantRestaurantRepository delegate,
  }) : _delegate = delegate;

  static const _listTtl = Duration(minutes: 5);
  static const _listKey = 'restaurants';

  final MerchantRestaurantRepository _delegate;
  final TtlCache<String, List<MerchantRestaurant>> _listCache = TtlCache();

  void clearCache() => _listCache.clear();

  @override
  Future<List<MerchantRestaurant>> listRestaurants({
    bool forceRefresh = false,
  }) {
    return _listCache.get(
      _listKey,
      loader: _delegate.listRestaurants,
      ttl: _listTtl,
      forceRefresh: forceRefresh,
    );
  }

  @override
  Future<MerchantRestaurant> createRestaurant(
    MerchantRestaurantRequest request, {
    XFile? backgroundFile,
    XFile? avatarFile,
  }) async {
    final created = await _delegate.createRestaurant(
      request,
      backgroundFile: backgroundFile,
      avatarFile: avatarFile,
    );
    _listCache.invalidate(_listKey);
    return created;
  }

  @override
  Future<MerchantRestaurant> updateRestaurant({
    required String restaurantId,
    required MerchantRestaurantRequest request,
    XFile? backgroundFile,
    XFile? avatarFile,
  }) async {
    final updated = await _delegate.updateRestaurant(
      restaurantId: restaurantId,
      request: request,
      backgroundFile: backgroundFile,
      avatarFile: avatarFile,
    );
    _listCache.invalidate(_listKey);
    return updated;
  }
}
