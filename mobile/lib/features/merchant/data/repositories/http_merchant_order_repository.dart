import '../../../../core/auth/auth_session_recovery.dart';
import '../../domain/models/merchant_order_detail.dart';
import '../../domain/models/merchant_order_summary.dart';
import '../../domain/repositories/merchant_order_repository.dart';
import '../data_sources/merchant_order_remote_data_source.dart';

class HttpMerchantOrderRepository implements MerchantOrderRepository {
  HttpMerchantOrderRepository({
    required MerchantOrderRemoteDataSource remoteDataSource,
    required AuthSessionRecovery sessionRecovery,
  }) : _remoteDataSource = remoteDataSource,
       _sessionRecovery = sessionRecovery;

  final MerchantOrderRemoteDataSource _remoteDataSource;
  final AuthSessionRecovery _sessionRecovery;

  @override
  Future<List<MerchantOrderSummary>> listRestaurantOrders(String restaurantId) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.listRestaurantOrders(
        accessToken: accessToken,
        restaurantId: restaurantId,
      );
    });
  }

  @override
  Future<MerchantOrderDetail> getOrderDetail(String orderId) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.getOrderDetail(
        accessToken: accessToken,
        orderId: orderId,
      );
    });
  }

  @override
  Future<MerchantOrderDetail> updateOrderStatus({
    required String orderId,
    required String status,
  }) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.updateOrderStatus(
        accessToken: accessToken,
        orderId: orderId,
        status: status,
      );
    });
  }
}
