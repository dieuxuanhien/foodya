import '../../domain/models/delivery_models.dart';
import '../../domain/repositories/delivery_repository.dart';

class UnavailableDeliveryRepository implements DeliveryRepository {
  const UnavailableDeliveryRepository();
  @override
  bool get isDemo => false;
  Future<T> _unavailable<T>() =>
      Future.error(const DeliveryFeatureUnavailableException());
  @override
  Future<DeliveryAssignment> acceptAssignment(String orderId) => _unavailable();
  @override
  Future<DeliveryBatch?> getActiveBatch() => _unavailable();
  @override
  Future<List<DeliveryAssignment>> getAssignments() => _unavailable();
  @override
  Future<DeliveryBatch> getBatch(String batchId) => _unavailable();
  @override
  Future<DeliveryEarningsReport> getEarnings({DateTime? from, DateTime? to}) =>
      _unavailable();
  @override
  Future<DeliveryRouteSummary> getRoute(String orderId) => _unavailable();
  @override
  Future<DeliverySession> getSession() => _unavailable();
  @override
  Future<DeliverySession> goOffline() => _unavailable();
  @override
  Future<DeliverySession> goOnline({double? lat, double? lng}) =>
      _unavailable();
  @override
  Future<DeliveryBatch> markDelivered(String batchId, String orderId) =>
      _unavailable();
  @override
  Future<DeliveryBatch> markFailed(String batchId, String orderId) =>
      _unavailable();
  @override
  Future<DeliveryBatch> markPickup(String batchId, String orderId) =>
      _unavailable();
  @override
  Future<DeliveryAssignment> rejectAssignment(String orderId) => _unavailable();
  @override
  Future<DeliveryAssignment> updateOrderStatus(
    String orderId,
    DeliveryOrderStatus status,
  ) => _unavailable();
}
