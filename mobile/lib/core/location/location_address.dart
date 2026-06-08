class LocationAddress {
  const LocationAddress({
    required this.latitude,
    required this.longitude,
    required this.address,
  });

  final double latitude;
  final double longitude;
  final String address;

  factory LocationAddress.fromJson(Map<String, dynamic> json) {
    return LocationAddress(
      latitude: _toDouble(json['lat']),
      longitude: _toDouble(json['lng']),
      address: json['address']?.toString() ?? '',
    );
  }

  static double _toDouble(Object? value) {
    if (value is num) {
      return value.toDouble();
    }

    return double.tryParse(value?.toString() ?? '') ?? 0;
  }
}
