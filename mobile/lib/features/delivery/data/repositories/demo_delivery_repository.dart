import '../../domain/models/delivery_models.dart';
import '../../domain/repositories/delivery_repository.dart';

enum DeliveryDemoScenario {
  offline('offline'),
  onlineEmpty('online-empty'),
  singleAssignment('single-assignment'),
  assignmentList('assignment-list'),
  orderDetail('order-detail'),
  acknowledged('assignment-acknowledged'),
  delivering('delivering'),
  completed('completed'),
  batchDetail('batch-detail'),
  batchProgress('batch-progress'),
  earnings('earnings'),
  walletPreview('wallet-preview'),
  networkError('network-error');

  const DeliveryDemoScenario(this.key);
  final String key;

  static DeliveryDemoScenario fromKey(String? value) {
    return values.firstWhere(
      (scenario) => scenario.key == value,
      orElse: () => singleAssignment,
    );
  }
}

class DemoDeliveryRepository implements DeliveryRepository {
  DemoDeliveryRepository({
    this.scenario = DeliveryDemoScenario.singleAssignment,
  }) : _session = _sessionFor(scenario),
       _assignments = _assignmentsFor(scenario),
       _batch = _batchFor(scenario);

  DeliveryDemoScenario scenario;
  DeliverySession _session;
  List<DeliveryAssignment> _assignments;
  DeliveryBatch? _batch;

  @override
  bool get isDemo => true;

  void setScenario(DeliveryDemoScenario value) {
    scenario = value;
    _session = _sessionFor(value);
    _assignments = _assignmentsFor(value);
    _batch = _batchFor(value);
  }

  Future<void> _delay() async {
    await Future<void>.delayed(const Duration(milliseconds: 120));
    if (scenario == DeliveryDemoScenario.networkError) {
      throw const DeliveryFeatureUnavailableException(
        'Demo network error. Choose another scenario and retry.',
      );
    }
  }

  @override
  Future<DeliverySession> getSession() async {
    await _delay();
    return _session;
  }

  @override
  Future<DeliverySession> goOnline({double? lat, double? lng}) async {
    await _delay();
    _session = const DeliverySession(
      sessionId: 'demo-session',
      status: DriverAvailability.online,
      activeOrderCount: 1,
      maxConcurrentOrders: 3,
      currentLat: 10.7769,
      currentLng: 106.7009,
      message: 'Demo driver is online.',
    );
    return _session;
  }

  @override
  Future<DeliverySession> goOffline() async {
    await _delay();
    _session = _session.copyWith(
      status:
          _session.activeOrderCount > 0
              ? DriverAvailability.goingOffline
              : DriverAvailability.offline,
      message:
          _session.activeOrderCount > 0
              ? 'Will go offline after the current delivery.'
              : 'Demo driver is offline.',
    );
    return _session;
  }

  @override
  Future<DeliverySession> updateLocation(double lat, double lng) async {
    await _delay();
    _session = DeliverySession(
      sessionId: _session.sessionId,
      status: _session.status,
      startedAt: _session.startedAt,
      endedAt: _session.endedAt,
      lastHeartbeat: _session.lastHeartbeat,
      currentLat: lat,
      currentLng: lng,
      h3Cell: _session.h3Cell,
      activeOrderCount: _session.activeOrderCount,
      maxConcurrentOrders: _session.maxConcurrentOrders,
      message: 'Demo location updated.',
    );
    return _session;
  }

  @override
  Future<List<DeliveryAssignment>> getAssignments() async {
    await _delay();
    return List.unmodifiable(_assignments);
  }

  @override
  Future<DeliveryAssignment> acceptAssignment(String orderId) async {
    await _delay();
    return _assignment(orderId);
  }

  @override
  Future<DeliveryAssignment> rejectAssignment(String orderId) async {
    await _delay();
    final assignment = _assignment(orderId);
    _assignments =
        _assignments.where((item) => item.orderId != orderId).toList();
    return assignment;
  }

  @override
  Future<DeliveryAssignment> updateOrderStatus(
    String orderId,
    DeliveryOrderStatus status,
  ) async {
    await _delay();
    final updated = _assignment(orderId).copyWith(status: status);
    _assignments = [
      for (final item in _assignments)
        if (item.orderId == orderId) updated else item,
    ];
    return updated;
  }

  @override
  Future<DeliveryBatch?> getActiveBatch() async {
    await _delay();
    return _batch;
  }

  @override
  Future<DeliveryBatch> getBatch(String batchId) async {
    await _delay();
    final batch = _batch;
    if (batch == null || (batchId != 'active' && batch.batchId != batchId)) {
      throw const DeliveryFeatureUnavailableException('Demo batch not found.');
    }
    return batch;
  }

  @override
  Future<DeliveryBatch> markPickup(String batchId, String orderId) {
    return _updateStop(batchId, orderId, DeliveryStopStatus.pickedUp);
  }

  @override
  Future<DeliveryBatch> markDelivered(String batchId, String orderId) {
    return _updateStop(batchId, orderId, DeliveryStopStatus.delivered);
  }

  @override
  Future<DeliveryBatch> markFailed(
    String batchId,
    String orderId, {
    String? reason,
  }) {
    return _updateStop(batchId, orderId, DeliveryStopStatus.failed);
  }

  Future<DeliveryBatch> _updateStop(
    String batchId,
    String orderId,
    DeliveryStopStatus status,
  ) async {
    await _delay();
    final batch = await getBatch(batchId);
    final now = DateTime.utc(2026, 11, 5, 8, 30);
    final stops = [
      for (final stop in batch.stops)
        if (stop.orderId == orderId)
          stop.copyWith(
            status: status,
            actualPickupAt: status == DeliveryStopStatus.pickedUp ? now : null,
            actualDeliveryAt:
                status == DeliveryStopStatus.delivered ? now : null,
          )
        else
          stop,
    ];
    _batch = batch.copyWith(
      status:
          stops.every((stop) => stop.status == DeliveryStopStatus.delivered)
              ? DeliveryBatchStatus.completed
              : DeliveryBatchStatus.active,
      stops: stops,
    );
    return _batch!;
  }

  @override
  Future<DeliveryRouteSummary> getRoute(String orderId) async {
    await _delay();
    return const DeliveryRouteSummary(
      distanceMeters: 8200,
      durationSeconds: 1860,
      waypoints: [
        DeliveryWaypoint(lat: 10.7769, lng: 106.7009, type: 'PICKUP'),
        DeliveryWaypoint(lat: 10.7890, lng: 106.7211, type: 'DELIVERY'),
      ],
    );
  }

  @override
  Future<DeliveryEarningsReport> getEarnings({
    DateTime? from,
    DateTime? to,
  }) async {
    await _delay();
    final end = to ?? DateTime.utc(2026, 11, 5);
    final start = from ?? end.subtract(const Duration(days: 30));
    return DeliveryEarningsReport(
      from: start,
      to: end,
      totalEarnings: 486000,
      codCashCollected: 1280000,
      deliveryCount: 18,
      entries: [
        DeliveryEarningsEntry(
          orderId: 'demo-order-1',
          orderCode: 'FY-241105-001',
          completedAt: DateTime.utc(2026, 11, 5, 4, 20),
          deliveryFee: 30000,
          platformMargin: 3000,
          driverEarning: 27000,
          paymentMethod: 'COD',
        ),
      ],
    );
  }

  DeliveryAssignment _assignment(String orderId) {
    return _assignments.firstWhere(
      (item) => item.orderId == orderId,
      orElse:
          () =>
              throw const DeliveryFeatureUnavailableException(
                'Demo assignment not found.',
              ),
    );
  }

  static DeliverySession _sessionFor(DeliveryDemoScenario scenario) {
    if (scenario == DeliveryDemoScenario.offline) {
      return const DeliverySession(
        status: DriverAvailability.offline,
        activeOrderCount: 0,
        maxConcurrentOrders: 3,
      );
    }
    return const DeliverySession(
      sessionId: 'demo-session',
      status: DriverAvailability.online,
      activeOrderCount: 1,
      maxConcurrentOrders: 3,
      currentLat: 10.7769,
      currentLng: 106.7009,
    );
  }

  static List<DeliveryAssignment> _assignmentsFor(
    DeliveryDemoScenario scenario,
  ) {
    if (scenario == DeliveryDemoScenario.offline ||
        scenario == DeliveryDemoScenario.onlineEmpty ||
        scenario == DeliveryDemoScenario.earnings ||
        scenario == DeliveryDemoScenario.walletPreview ||
        scenario == DeliveryDemoScenario.networkError) {
      return [];
    }
    final status = switch (scenario) {
      DeliveryDemoScenario.delivering => DeliveryOrderStatus.delivering,
      DeliveryDemoScenario.completed => DeliveryOrderStatus.success,
      _ => DeliveryOrderStatus.assigned,
    };
    return [
      DeliveryAssignment(
        orderId: 'demo-order-1',
        orderCode: 'FY-241105-001',
        customerName: 'Nguyễn Minh Anh',
        restaurantName: 'Bếp Việt Sài Gòn',
        status: status,
        paymentStatus: 'UNPAID',
        paymentMethod: 'COD',
        totalAmount: 186000,
        deliveryAddress: '128 Nguyễn Huệ, Phường Sài Gòn, TP. Hồ Chí Minh',
        deliveryLatitude: 10.7742,
        deliveryLongitude: 106.7038,
      ),
    ];
  }

  static DeliveryBatch? _batchFor(DeliveryDemoScenario scenario) {
    if (scenario != DeliveryDemoScenario.batchDetail &&
        scenario != DeliveryDemoScenario.batchProgress) {
      return null;
    }
    final progressed = scenario == DeliveryDemoScenario.batchProgress;
    return DeliveryBatch(
      batchId: 'BATCH-DEMO-2404',
      status: DeliveryBatchStatus.active,
      totalDistanceMeters: 12800,
      totalDurationSeconds: 2760,
      createdAt: DateTime.utc(2026, 11, 5, 7),
      stops: [
        DeliveryStop(
          id: 'stop-1',
          orderId: 'demo-order-1',
          pickupIndex: 1,
          deliveryIndex: 1,
          status:
              progressed
                  ? DeliveryStopStatus.pickedUp
                  : DeliveryStopStatus.pending,
          restaurantName: 'Bếp Việt Sài Gòn',
          deliveryAddress: '128 Nguyễn Huệ, Phường Sài Gòn, TP. Hồ Chí Minh',
        ),
        const DeliveryStop(
          id: 'stop-2',
          orderId: 'demo-order-2',
          pickupIndex: 2,
          deliveryIndex: 2,
          status: DeliveryStopStatus.pending,
          restaurantName: 'Cơm Nhà Quận 3',
          deliveryAddress: '45 Võ Văn Tần, Phường Xuân Hòa, TP. Hồ Chí Minh',
        ),
      ],
    );
  }
}
