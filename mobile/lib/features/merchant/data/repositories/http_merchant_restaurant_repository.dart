import 'package:image_picker/image_picker.dart';

import '../../../../core/auth/auth_session_recovery.dart';
import '../../domain/models/merchant_restaurant.dart';
import '../../domain/models/merchant_restaurant_request.dart';
import '../../domain/repositories/merchant_restaurant_repository.dart';
import '../data_sources/merchant_restaurant_remote_data_source.dart';

class HttpMerchantRestaurantRepository implements MerchantRestaurantRepository {
  HttpMerchantRestaurantRepository({
    required MerchantRestaurantRemoteDataSource remoteDataSource,
    required AuthSessionRecovery sessionRecovery,
  }) : _remoteDataSource = remoteDataSource,
       _sessionRecovery = sessionRecovery;

  final MerchantRestaurantRemoteDataSource _remoteDataSource;
  final AuthSessionRecovery _sessionRecovery;

  @override
  Future<List<MerchantRestaurant>> listRestaurants() {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.listRestaurants(accessToken: accessToken);
    });
  }

  @override
  Future<MerchantRestaurant> createRestaurant(
    MerchantRestaurantRequest request, {
    XFile? backgroundFile,
    XFile? avatarFile,
  }) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.createRestaurant(
        accessToken: accessToken,
        request: request,
        backgroundFile: backgroundFile,
        avatarFile: avatarFile,
      );
    });
  }

  @override
  Future<MerchantRestaurant> updateRestaurant({
    required String restaurantId,
    required MerchantRestaurantRequest request,
    XFile? backgroundFile,
    XFile? avatarFile,
  }) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.updateRestaurant(
        accessToken: accessToken,
        restaurantId: restaurantId,
        request: request,
        backgroundFile: backgroundFile,
        avatarFile: avatarFile,
      );
    });
  }
}
