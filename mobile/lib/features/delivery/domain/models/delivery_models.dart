import 'dart:convert';

import 'package:equatable/equatable.dart';

enum DriverAvailability {
  offline,
  online,
  onDelivery,
  busy,
  goingOffline,
  suspended,
  unknown;

  static DriverAvailability fromApi(String? value) => switch (value
      ?.toUpperCase()) {
    'OFFLINE' => offline,
    'ONLINE' => online,
    'ON_DELIVERY' => onDelivery,
    'BUSY' => busy,
    'GOING_OFFLINE' => goingOffline,
    'SUSPENDED' => suspended,
    _ => unknown,
  };

  String get label => switch (this) {
    offline => 'Offline',
    online => 'Online',
    onDelivery => 'On delivery',
    busy => 'Busy',
    goingOffline => 'Going offline',
    suspended => 'Suspended',
    unknown => 'Unknown',
  };
}

enum DeliveryOrderStatus {
  accepted,
  assigned,
  preparing,
  delivering,
  success,
  failed,
  cancelled,
  unknown;

  static DeliveryOrderStatus fromApi(String? value) => switch (value
      ?.toUpperCase()) {
    'ACCEPTED' => accepted,
    'ASSIGNED' => assigned,
    'PREPARING' => preparing,
    'DELIVERING' => delivering,
    'SUCCESS' => success,
    'FAILED' => failed,
    'CANCELLED' => cancelled,
    _ => unknown,
  };

  String get label => switch (this) {
    accepted => 'Awaiting assignment',
    assigned => 'Assigned',
    preparing => 'Preparing',
    delivering => 'Delivering',
    success => 'Delivered',
    failed => 'Failed',
    cancelled => 'Cancelled',
    unknown => 'Unknown',
  };
}

enum DeliveryBatchStatus {
  pending,
  active,
  completed,
  cancelled,
  partial,
  unknown;

  static DeliveryBatchStatus fromApi(String? value) => switch (value
      ?.toUpperCase()) {
    'PENDING' => pending,
    'ACTIVE' => active,
    'COMPLETED' => completed,
    'CANCELLED' => cancelled,
    'PARTIAL' => partial,
    _ => unknown,
  };
}

enum DeliveryStopStatus {
  pending,
  pickedUp,
  delivering,
  delivered,
  failed,
  unknown;

  static DeliveryStopStatus fromApi(String? value) => switch (value
      ?.toUpperCase()) {
    'PENDING' => pending,
    'PICKED_UP' || 'PICKUP_COMPLETE' => pickedUp,
    'DELIVERING' => delivering,
    'DELIVERED' || 'DELIVERY_COMPLETE' => delivered,
    'FAILED' => failed,
    _ => unknown,
  };
}

class DeliverySession extends Equatable {
  const DeliverySession({
    required this.status,
    required this.activeOrderCount,
    required this.maxConcurrentOrders,
    this.sessionId,
    this.startedAt,
    this.endedAt,
    this.lastHeartbeat,
    this.currentLat,
    this.currentLng,
    this.h3Cell,
    this.message,
  });

  factory DeliverySession.fromJson(Map<String, dynamic> json) {
    return DeliverySession(
      sessionId: json['sessionId']?.toString(),
      status: DriverAvailability.fromApi(json['status']?.toString()),
      startedAt: _date(json['startedAt']),
      endedAt: _date(json['endedAt']),
      lastHeartbeat: _date(json['lastHeartbeat']),
      currentLat: _number(json['currentLat']),
      currentLng: _number(json['currentLng']),
      h3Cell: json['h3Cell']?.toString(),
      activeOrderCount: _integer(json['activeOrderCount']),
      maxConcurrentOrders: _integer(json['maxConcurrentOrders']),
      message: json['message']?.toString(),
    );
  }

  final String? sessionId;
  final DriverAvailability status;
  final DateTime? startedAt;
  final DateTime? endedAt;
  final DateTime? lastHeartbeat;
  final double? currentLat;
  final double? currentLng;
  final String? h3Cell;
  final int activeOrderCount;
  final int maxConcurrentOrders;
  final String? message;

  bool get isOnline => switch (status) {
    DriverAvailability.online ||
    DriverAvailability.onDelivery ||
    DriverAvailability.busy ||
    DriverAvailability.goingOffline => true,
    _ => false,
  };

  bool get canToggleAvailability =>
      status != DriverAvailability.suspended &&
      status != DriverAvailability.unknown;

  DeliverySession copyWith({DriverAvailability? status, String? message}) {
    return DeliverySession(
      sessionId: sessionId,
      status: status ?? this.status,
      startedAt: startedAt,
      endedAt: endedAt,
      lastHeartbeat: lastHeartbeat,
      currentLat: currentLat,
      currentLng: currentLng,
      h3Cell: h3Cell,
      activeOrderCount: activeOrderCount,
      maxConcurrentOrders: maxConcurrentOrders,
      message: message ?? this.message,
    );
  }

  @override
  List<Object?> get props => [
    sessionId,
    status,
    startedAt,
    endedAt,
    lastHeartbeat,
    currentLat,
    currentLng,
    h3Cell,
    activeOrderCount,
    maxConcurrentOrders,
    message,
  ];
}

class DeliveryAssignment extends Equatable {
  const DeliveryAssignment({
    required this.orderId,
    required this.orderCode,
    required this.customerName,
    required this.restaurantName,
    required this.status,
    required this.paymentStatus,
    required this.totalAmount,
    this.restaurantId,
    this.customerUserId,
    this.paymentMethod,
    this.subtotalAmount,
    this.deliveryFee,
    this.deliveryAddress,
    this.deliveryLatitude,
    this.deliveryLongitude,
  });

  factory DeliveryAssignment.fromJson(Map<String, dynamic> json) {
    return DeliveryAssignment(
      orderId: json['orderId']?.toString() ?? '',
      orderCode: json['orderCode']?.toString() ?? '',
      restaurantId: json['restaurantId']?.toString(),
      restaurantName: json['restaurantName']?.toString() ?? '',
      customerUserId: json['customerUserId']?.toString(),
      customerName: json['customerName']?.toString() ?? '',
      status: DeliveryOrderStatus.fromApi(json['status']?.toString()),
      paymentMethod: json['paymentMethod']?.toString(),
      paymentStatus: json['paymentStatus']?.toString() ?? 'UNKNOWN',
      subtotalAmount: _number(json['subtotalAmount']),
      deliveryFee: _number(json['deliveryFee']),
      totalAmount: _number(json['totalAmount']) ?? 0,
      deliveryAddress: json['deliveryAddress']?.toString(),
      deliveryLatitude: _number(json['deliveryLatitude']),
      deliveryLongitude: _number(json['deliveryLongitude']),
    );
  }

  final String orderId;
  final String orderCode;
  final String? restaurantId;
  final String restaurantName;
  final String? customerUserId;
  final String customerName;
  final DeliveryOrderStatus status;
  final String? paymentMethod;
  final String paymentStatus;
  final double? subtotalAmount;
  final double? deliveryFee;
  final double totalAmount;
  final String? deliveryAddress;
  final double? deliveryLatitude;
  final double? deliveryLongitude;

  DeliveryAssignment copyWith({DeliveryOrderStatus? status}) =>
      DeliveryAssignment(
        orderId: orderId,
        orderCode: orderCode,
        restaurantId: restaurantId,
        restaurantName: restaurantName,
        customerUserId: customerUserId,
        customerName: customerName,
        status: status ?? this.status,
        paymentMethod: paymentMethod,
        paymentStatus: paymentStatus,
        subtotalAmount: subtotalAmount,
        deliveryFee: deliveryFee,
        totalAmount: totalAmount,
        deliveryAddress: deliveryAddress,
        deliveryLatitude: deliveryLatitude,
        deliveryLongitude: deliveryLongitude,
      );

  @override
  List<Object?> get props => [
    orderId,
    orderCode,
    restaurantId,
    restaurantName,
    customerUserId,
    customerName,
    status,
    paymentMethod,
    paymentStatus,
    subtotalAmount,
    deliveryFee,
    totalAmount,
    deliveryAddress,
    deliveryLatitude,
    deliveryLongitude,
  ];
}

class DeliveryStop extends Equatable {
  const DeliveryStop({
    required this.id,
    required this.orderId,
    required this.pickupIndex,
    required this.deliveryIndex,
    required this.status,
    required this.restaurantName,
    required this.deliveryAddress,
    this.estimatedPickupAt,
    this.estimatedDeliveryAt,
    this.actualPickupAt,
    this.actualDeliveryAt,
  });

  factory DeliveryStop.fromJson(Map<String, dynamic> json) {
    return DeliveryStop(
      id: json['id']?.toString() ?? '',
      orderId: json['orderId']?.toString() ?? '',
      pickupIndex: _integer(json['pickupIndex']),
      deliveryIndex: _integer(json['deliveryIndex']),
      status: DeliveryStopStatus.fromApi(json['itemStatus']?.toString()),
      restaurantName:
          json['restaurantName']?.toString() ?? 'Restaurant unavailable',
      deliveryAddress:
          json['deliveryAddress']?.toString() ?? 'Address unavailable',
      estimatedPickupAt: _date(json['estimatedPickupAt']),
      estimatedDeliveryAt: _date(json['estimatedDeliveryAt']),
      actualPickupAt: _date(json['actualPickupAt']),
      actualDeliveryAt: _date(json['actualDeliveryAt']),
    );
  }

  final String id;
  final String orderId;
  final int pickupIndex;
  final int deliveryIndex;
  final DeliveryStopStatus status;
  final String restaurantName;
  final String deliveryAddress;
  final DateTime? estimatedPickupAt;
  final DateTime? estimatedDeliveryAt;
  final DateTime? actualPickupAt;
  final DateTime? actualDeliveryAt;

  DeliveryStop copyWith({
    DeliveryStopStatus? status,
    DateTime? actualPickupAt,
    DateTime? actualDeliveryAt,
  }) => DeliveryStop(
    id: id,
    orderId: orderId,
    pickupIndex: pickupIndex,
    deliveryIndex: deliveryIndex,
    status: status ?? this.status,
    restaurantName: restaurantName,
    deliveryAddress: deliveryAddress,
    estimatedPickupAt: estimatedPickupAt,
    estimatedDeliveryAt: estimatedDeliveryAt,
    actualPickupAt: actualPickupAt ?? this.actualPickupAt,
    actualDeliveryAt: actualDeliveryAt ?? this.actualDeliveryAt,
  );

  @override
  List<Object?> get props => [
    id,
    orderId,
    pickupIndex,
    deliveryIndex,
    status,
    restaurantName,
    deliveryAddress,
    estimatedPickupAt,
    estimatedDeliveryAt,
    actualPickupAt,
    actualDeliveryAt,
  ];
}

class DeliveryBatch extends Equatable {
  const DeliveryBatch({
    required this.batchId,
    required this.status,
    required this.stops,
    this.totalDistanceMeters,
    this.totalDurationSeconds,
    this.createdAt,
    this.startedAt,
    this.completedAt,
    this.pickupSequence = const [],
    this.deliverySequence = const [],
  });

  factory DeliveryBatch.fromJson(Map<String, dynamic> json) {
    final items = json['items'];
    return DeliveryBatch(
      batchId: json['batchId']?.toString() ?? '',
      status: DeliveryBatchStatus.fromApi(json['status']?.toString()),
      pickupSequence: _decodeSequence(json['pickupSequence']),
      deliverySequence: _decodeSequence(json['deliverySequence']),
      totalDistanceMeters: _nullableInteger(json['totalDistanceMeters']),
      totalDurationSeconds: _nullableInteger(json['totalDurationSeconds']),
      createdAt: _date(json['createdAt']),
      startedAt: _date(json['startedAt']),
      completedAt: _date(json['completedAt']),
      stops:
          items is List
              ? items
                  .whereType<Map>()
                  .map(
                    (item) => DeliveryStop.fromJson(
                      item.map((key, value) => MapEntry(key.toString(), value)),
                    ),
                  )
                  .toList(growable: false)
              : const [],
    );
  }

  final String batchId;
  final DeliveryBatchStatus status;
  final List<DeliveryStop> stops;
  final int? totalDistanceMeters;
  final int? totalDurationSeconds;
  final DateTime? createdAt;
  final DateTime? startedAt;
  final DateTime? completedAt;
  final List<DeliverySequenceStop> pickupSequence;
  final List<DeliverySequenceStop> deliverySequence;

  DeliveryBatch copyWith({
    DeliveryBatchStatus? status,
    List<DeliveryStop>? stops,
  }) => DeliveryBatch(
    batchId: batchId,
    status: status ?? this.status,
    stops: stops ?? this.stops,
    totalDistanceMeters: totalDistanceMeters,
    totalDurationSeconds: totalDurationSeconds,
    createdAt: createdAt,
    startedAt: startedAt,
    completedAt: completedAt,
    pickupSequence: pickupSequence,
    deliverySequence: deliverySequence,
  );

  @override
  List<Object?> get props => [
    batchId,
    status,
    stops,
    totalDistanceMeters,
    totalDurationSeconds,
    createdAt,
    startedAt,
    completedAt,
    pickupSequence,
    deliverySequence,
  ];
}

class DeliverySequenceStop extends Equatable {
  const DeliverySequenceStop({
    required this.orderId,
    this.lat,
    this.lng,
    this.estimatedArrivalSeconds,
    this.serviceTimeSeconds,
  });

  final String orderId;
  final double? lat;
  final double? lng;
  final int? estimatedArrivalSeconds;
  final int? serviceTimeSeconds;

  bool get hasUsableCoordinates =>
      lat != null && lng != null && !(lat == 0 && lng == 0);

  @override
  List<Object?> get props => [
    orderId,
    lat,
    lng,
    estimatedArrivalSeconds,
    serviceTimeSeconds,
  ];
}

class DeliveryRouteSummary extends Equatable {
  const DeliveryRouteSummary({
    required this.distanceMeters,
    required this.durationSeconds,
    required this.waypoints,
    this.encodedPolyline,
    this.calculatedAt,
  });

  factory DeliveryRouteSummary.fromJson(Map<String, dynamic> json) {
    final rawWaypoints = json['waypoints'];
    return DeliveryRouteSummary(
      encodedPolyline: json['encodedPolyline']?.toString(),
      distanceMeters: _integer(json['distanceMeters']),
      durationSeconds: _integer(json['durationSeconds']),
      waypoints:
          rawWaypoints is List
              ? rawWaypoints
                  .whereType<Map>()
                  .map((item) {
                    final mapped = item.map(
                      (key, value) => MapEntry(key.toString(), value),
                    );
                    return DeliveryWaypoint(
                      lat: _number(mapped['lat']) ?? 0,
                      lng: _number(mapped['lng']) ?? 0,
                      type: mapped['type']?.toString() ?? 'UNKNOWN',
                    );
                  })
                  .toList(growable: false)
              : const [],
      calculatedAt: _date(json['calculatedAt']),
    );
  }
  final String? encodedPolyline;
  final int distanceMeters;
  final int durationSeconds;
  final List<DeliveryWaypoint> waypoints;
  final DateTime? calculatedAt;
  @override
  List<Object?> get props => [
    encodedPolyline,
    distanceMeters,
    durationSeconds,
    waypoints,
    calculatedAt,
  ];
}

class DeliveryWaypoint extends Equatable {
  const DeliveryWaypoint({
    required this.lat,
    required this.lng,
    required this.type,
  });
  final double lat;
  final double lng;
  final String type;
  @override
  List<Object?> get props => [lat, lng, type];
}

class DeliveryEarningsEntry extends Equatable {
  const DeliveryEarningsEntry({
    required this.orderId,
    required this.orderCode,
    required this.completedAt,
    required this.deliveryFee,
    required this.platformMargin,
    required this.driverEarning,
    required this.paymentMethod,
  });

  factory DeliveryEarningsEntry.fromJson(Map<String, dynamic> json) {
    return DeliveryEarningsEntry(
      orderId: json['orderId']?.toString() ?? '',
      orderCode: json['orderCode']?.toString() ?? '',
      completedAt:
          _date(json['completedAt']) ??
          DateTime.fromMillisecondsSinceEpoch(0, isUtc: true),
      deliveryFee: _number(json['deliveryFee']) ?? 0,
      platformMargin: _number(json['platformMargin']) ?? 0,
      driverEarning: _number(json['driverEarning']) ?? 0,
      paymentMethod: json['paymentMethod']?.toString() ?? 'UNKNOWN',
    );
  }
  final String orderId;
  final String orderCode;
  final DateTime completedAt;
  final double deliveryFee;
  final double platformMargin;
  final double driverEarning;
  final String paymentMethod;
  @override
  List<Object?> get props => [
    orderId,
    orderCode,
    completedAt,
    deliveryFee,
    platformMargin,
    driverEarning,
    paymentMethod,
  ];
}

class DeliveryEarningsReport extends Equatable {
  const DeliveryEarningsReport({
    required this.from,
    required this.to,
    required this.totalEarnings,
    required this.codCashCollected,
    required this.deliveryCount,
    required this.entries,
  });

  factory DeliveryEarningsReport.fromJson(Map<String, dynamic> json) {
    final rawEntries = json['entries'];
    return DeliveryEarningsReport(
      from:
          _date(json['from']) ??
          DateTime.fromMillisecondsSinceEpoch(0, isUtc: true),
      to:
          _date(json['to']) ??
          DateTime.fromMillisecondsSinceEpoch(0, isUtc: true),
      totalEarnings: _number(json['totalEarnings']) ?? 0,
      codCashCollected: _number(json['codCashCollected']) ?? 0,
      deliveryCount: _integer(json['deliveryCount']),
      entries:
          rawEntries is List
              ? rawEntries
                  .whereType<Map>()
                  .map(
                    (item) => DeliveryEarningsEntry.fromJson(
                      item.map((key, value) => MapEntry(key.toString(), value)),
                    ),
                  )
                  .toList(growable: false)
              : const [],
    );
  }
  final DateTime from;
  final DateTime to;
  final double totalEarnings;
  final double codCashCollected;
  final int deliveryCount;
  final List<DeliveryEarningsEntry> entries;
  @override
  List<Object?> get props => [
    from,
    to,
    totalEarnings,
    codCashCollected,
    deliveryCount,
    entries,
  ];
}

double? _number(Object? value) =>
    value is num ? value.toDouble() : double.tryParse(value?.toString() ?? '');
int _integer(Object? value) =>
    value is num ? value.toInt() : int.tryParse(value?.toString() ?? '') ?? 0;
int? _nullableInteger(Object? value) => value == null ? null : _integer(value);
DateTime? _date(Object? value) =>
    value == null ? null : DateTime.tryParse(value.toString())?.toUtc();

List<DeliverySequenceStop> _decodeSequence(Object? raw) {
  if (raw == null || raw.toString().trim().isEmpty) return const [];
  try {
    final decoded = raw is String ? jsonDecode(raw) : raw;
    if (decoded is! List) return const [];
    return decoded
        .whereType<Map>()
        .map((item) {
          final mapped = item.map(
            (key, value) => MapEntry(key.toString(), value),
          );
          final lat = _number(mapped['lat']);
          final lng = _number(mapped['lng']);
          return DeliverySequenceStop(
            orderId:
                mapped['orderId']?.toString() ?? mapped['id']?.toString() ?? '',
            lat: lat == 0 && lng == 0 ? null : lat,
            lng: lat == 0 && lng == 0 ? null : lng,
            estimatedArrivalSeconds: _nullableInteger(
              mapped['estimatedArrivalSec'],
            ),
            serviceTimeSeconds: _nullableInteger(mapped['serviceTimeSec']),
          );
        })
        .where((item) => item.orderId.isNotEmpty)
        .toList(growable: false);
  } catch (_) {
    return const [];
  }
}
