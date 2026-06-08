class OrderTrackingPoint {
  const OrderTrackingPoint({
    required this.lat,
    required this.lng,
    required this.recordedAt,
  });

  final double lat;
  final double lng;
  final DateTime recordedAt;

  factory OrderTrackingPoint.fromJson(Map<String, dynamic> json) {
    return OrderTrackingPoint(
      lat: _toDouble(json['lat']),
      lng: _toDouble(json['lng']),
      recordedAt: _toDateTime(json['recordedAt']),
    );
  }

  static double _toDouble(Object? value) {
    if (value is num) {
      return value.toDouble();
    }
    return double.tryParse(value?.toString() ?? '') ?? 0;
  }

  static DateTime _toDateTime(Object? value) {
    if (value is DateTime) {
      return value;
    }
    final raw = value?.toString();
    if (raw == null || raw.trim().isEmpty) {
      return DateTime.fromMillisecondsSinceEpoch(0, isUtc: true);
    }
    return DateTime.tryParse(raw) ??
        DateTime.fromMillisecondsSinceEpoch(0, isUtc: true);
  }
}
