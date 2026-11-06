import 'dart:async';

import '../../../../core/auth/auth_session_recovery.dart';
import '../../../../core/network/api_exception.dart';
import '../../domain/models/delivery_models.dart';
import '../../domain/repositories/delivery_repository.dart';
import '../data_sources/delivery_remote_data_source.dart';

typedef SessionExpiredCallback = FutureOr<void> Function();

class HttpDeliveryRepository implements DeliveryRepository {
  HttpDeliveryRepository({
    required DeliveryRemoteDataSource remoteDataSource,
    required AuthSessionRecovery sessionRecovery,
    SessionExpiredCallback? onSessionExpired,
  }) : _remote = remoteDataSource,
       _sessionRecovery = sessionRecovery,
       _onSessionExpired = onSessionExpired;

  final DeliveryRemoteDataSource _remote;
  final AuthSessionRecovery _sessionRecovery;
  final SessionExpiredCallback? _onSessionExpired;

  @override
  bool get isDemo => false;

  Future<T> _authorized<T>(Future<T> Function(String token) action) async {
    try {
      return await _sessionRecovery.runAuthorized(action);
    } on ApiException catch (error) {
      if (error.statusCode == 401) await _onSessionExpired?.call();
      rethrow;
    }
  }

  @override
  Future<DeliverySession> getSession() => _authorized(
    (token) async => DeliverySession.fromJson(await _remote.getSession(token)),
  );

  @override
  Future<DeliverySession> goOnline({double? lat, double? lng}) => _authorized(
    (token) async => DeliverySession.fromJson(
      await _remote.goOnline(token, lat: lat, lng: lng),
    ),
  );

  @override
  Future<DeliverySession> goOffline() => _authorized(
    (token) async => DeliverySession.fromJson(await _remote.goOffline(token)),
  );

  @override
  Future<DeliverySession> updateLocation(double lat, double lng) => _authorized(
    (token) async =>
        DeliverySession.fromJson(await _remote.updateLocation(token, lat, lng)),
  );

  @override
  Future<List<DeliveryAssignment>> getAssignments() => _authorized(
    (token) async => (await _remote.getAssignments(
      token,
    )).map(DeliveryAssignment.fromJson).toList(growable: false),
  );

  @override
  Future<DeliveryAssignment> acceptAssignment(String orderId) => _authorized(
    (token) async => DeliveryAssignment.fromJson(
      await _remote.acceptAssignment(token, orderId),
    ),
  );

  @override
  Future<DeliveryAssignment> rejectAssignment(String orderId) => _authorized(
    (token) async => DeliveryAssignment.fromJson(
      await _remote.rejectAssignment(token, orderId),
    ),
  );

  @override
  Future<DeliveryAssignment> updateOrderStatus(
    String orderId,
    DeliveryOrderStatus status,
  ) => _authorized(
    (token) async => DeliveryAssignment.fromJson(
      await _remote.updateOrderStatus(
        token,
        orderId,
        status.name.toUpperCase(),
      ),
    ),
  );

  @override
  Future<DeliveryBatch?> getActiveBatch() => _authorized((token) async {
    try {
      return DeliveryBatch.fromJson(await _remote.getActiveBatch(token));
    } on ApiException catch (error) {
      final noActiveBatch =
          error.statusCode == 404 &&
          error.code == 'RESOURCE_NOT_FOUND' &&
          error.message.toLowerCase().contains('no active batch');
      if (noActiveBatch) return null;
      rethrow;
    }
  });

  @override
  Future<DeliveryBatch> getBatch(String batchId) => _authorized(
    (token) async =>
        DeliveryBatch.fromJson(await _remote.getBatch(token, batchId)),
  );

  @override
  Future<DeliveryBatch> markPickup(String batchId, String orderId) =>
      _mutateBatch(
        batchId,
        (token) => _remote.markPickup(token, batchId, orderId),
      );

  @override
  Future<DeliveryBatch> markDelivered(String batchId, String orderId) =>
      _mutateBatch(
        batchId,
        (token) => _remote.markDelivered(token, batchId, orderId),
      );

  @override
  Future<DeliveryBatch> markFailed(
    String batchId,
    String orderId, {
    String? reason,
  }) => _mutateBatch(
    batchId,
    (token) => _remote.markFailed(token, batchId, orderId, reason: reason),
  );

  Future<DeliveryBatch> _mutateBatch(
    String batchId,
    Future<void> Function(String token) mutation,
  ) => _authorized((token) async {
    await mutation(token);
    return DeliveryBatch.fromJson(await _remote.getBatch(token, batchId));
  });

  @override
  Future<DeliveryRouteSummary> getRoute(String orderId) => _authorized(
    (token) async =>
        DeliveryRouteSummary.fromJson(await _remote.getRoute(token, orderId)),
  );

  @override
  Future<DeliveryEarningsReport> getEarnings({DateTime? from, DateTime? to}) =>
      _authorized(
        (token) async => DeliveryEarningsReport.fromJson(
          await _remote.getEarnings(token, from: from, to: to),
        ),
      );
}
