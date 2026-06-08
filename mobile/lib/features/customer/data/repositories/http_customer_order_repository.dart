import '../../../../core/auth/auth_session_recovery.dart';
import '../../domain/models/create_order_request.dart';
import '../../domain/models/order_cost_review.dart';
import '../../domain/models/order_created.dart';
import '../../domain/models/order_detail.dart';
import '../../domain/models/order_review.dart';
import '../../domain/models/order_summary.dart';
import '../../domain/models/order_tracking_point.dart';
import '../../domain/repositories/customer_order_repository.dart';
import '../data_sources/customer_order_remote_data_source.dart';

class HttpCustomerOrderRepository implements CustomerOrderRepository {
  HttpCustomerOrderRepository({
    required CustomerOrderRemoteDataSource remoteDataSource,
    required AuthSessionRecovery sessionRecovery,
  }) : _remoteDataSource = remoteDataSource,
       _sessionRecovery = sessionRecovery;

  final CustomerOrderRemoteDataSource _remoteDataSource;
  final AuthSessionRecovery _sessionRecovery;

  @override
  Future<OrderCostReview> reviewOrderCost(CreateOrderRequest request) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.reviewOrderCost(
        accessToken: accessToken,
        request: request,
      );
    });
  }

  @override
  Future<OrderCreated> createOrder({
    required CreateOrderRequest request,
    required String idempotencyKey,
  }) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.createOrder(
        accessToken: accessToken,
        request: request,
        idempotencyKey: idempotencyKey,
      );
    });
  }

  @override
  Future<List<OrderSummary>> listOrders() {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.listOrders(accessToken: accessToken);
    });
  }

  @override
  Future<OrderDetail> getOrderDetail(String orderId) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.getOrderDetail(
        accessToken: accessToken,
        orderId: orderId,
      );
    });
  }

  @override
  Future<OrderDetail> cancelOrder(String orderId, {String? reason}) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.cancelOrder(
        accessToken: accessToken,
        orderId: orderId,
        reason: reason,
      );
    });
  }

  @override
  Future<List<OrderTrackingPoint>> getTrackingPoints(String orderId) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.trackingPoints(
        accessToken: accessToken,
        orderId: orderId,
      );
    });
  }

  @override
  Future<OrderReview> createReview({
    required String orderId,
    required int stars,
    String? comment,
  }) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.createReview(
        accessToken: accessToken,
        orderId: orderId,
        stars: stars,
        comment: comment,
      );
    });
  }
}
