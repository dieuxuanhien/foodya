import '../models/create_order_request.dart';
import '../models/order_cost_review.dart';
import '../models/order_created.dart';
import '../models/order_detail.dart';
import '../models/order_summary.dart';
import '../models/order_tracking_point.dart';

abstract class CustomerOrderRepository {
  Future<OrderCostReview> reviewOrderCost(CreateOrderRequest request);

  Future<OrderCreated> createOrder({
    required CreateOrderRequest request,
    required String idempotencyKey,
  });

  Future<List<OrderSummary>> listOrders();

  Future<OrderDetail> getOrderDetail(String orderId);

  Future<OrderDetail> cancelOrder(String orderId, {String? reason});

  Future<List<OrderTrackingPoint>> getTrackingPoints(String orderId);
}
