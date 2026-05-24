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
  Future<MerchantRestaurant> createRestaurant(
    MerchantRestaurantRequest request,
  ) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.createRestaurant(
        accessToken: accessToken,
        request: request,
      );
    });
  }

  @override
  Future<MerchantRestaurant> updateRestaurant({
    required String restaurantId,
    required MerchantRestaurantRequest request,
  }) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.updateRestaurant(
        accessToken: accessToken,
        restaurantId: restaurantId,
        request: request,
      );
    });
  }
}
