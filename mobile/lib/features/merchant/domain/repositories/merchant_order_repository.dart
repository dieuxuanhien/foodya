import '../models/merchant_order_detail.dart';
import '../models/merchant_order_summary.dart';

abstract class MerchantOrderRepository {
  Future<List<MerchantOrderSummary>> listRestaurantOrders(String restaurantId);

  Future<MerchantOrderDetail> getOrderDetail(String orderId);

  Future<MerchantOrderDetail> updateOrderStatus({
    required String orderId,
    required String status,
  });
}
