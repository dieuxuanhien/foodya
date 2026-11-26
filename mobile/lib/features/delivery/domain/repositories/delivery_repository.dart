import '../models/delivery_models.dart';

abstract class DeliveryRepository {
  bool get isDemo;

  Future<DeliverySession> getSession();
  Future<DeliverySession> goOnline({double? lat, double? lng});
  Future<DeliverySession> goOffline();
  Future<DeliverySession> updateLocation(double lat, double lng);
  Future<List<DeliveryAssignment>> getAssignments();
  Future<DeliveryAssignment> acceptAssignment(String orderId);
  Future<DeliveryAssignment> rejectAssignment(String orderId);
  Future<DeliveryAssignment> updateOrderStatus(
    String orderId,
    DeliveryOrderStatus status,
  );
  Future<DeliveryBatch?> getActiveBatch();
  Future<DeliveryBatch> getBatch(String batchId);
  Future<DeliveryBatch> markPickup(String batchId, String orderId);
  Future<DeliveryBatch> markDelivered(String batchId, String orderId);
  Future<DeliveryBatch> markFailed(
    String batchId,
    String orderId, {
    String? reason,
  });
  Future<DeliveryRouteSummary> getRoute(String orderId);
  Future<DeliveryEarningsReport> getEarnings({DateTime? from, DateTime? to});
}

class DeliveryFeatureUnavailableException implements Exception {
  const DeliveryFeatureUnavailableException([
    this.message = 'Live Delivery integration is deferred to Phase 5.',
  ]);
  final String message;
  @override
  String toString() => message;
}
