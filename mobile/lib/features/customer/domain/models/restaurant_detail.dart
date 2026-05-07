class RestaurantDetail {
  const RestaurantDetail({
    required this.id,
    required this.name,
    required this.cuisineType,
    required this.description,
    required this.backgroundImageUrl,
    required this.avatarImageUrl,
    required this.addressLine,
    required this.latitude,
    required this.longitude,
    required this.avgRating,
    required this.reviewCount,
    required this.open,
    required this.maxDeliveryKm,
  });

  final String id;
  final String name;
  final String cuisineType;
  final String? description;
  final String? backgroundImageUrl;
  final String? avatarImageUrl;
  final String addressLine;
  final double latitude;
  final double longitude;
  final double avgRating;
  final int reviewCount;
  final bool open;
  final double maxDeliveryKm;

  factory RestaurantDetail.fromJson(Map<String, dynamic> json) {
    return RestaurantDetail(
      id: json['id']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      cuisineType: json['cuisineType']?.toString() ?? '',
      description: json['description']?.toString(),
      backgroundImageUrl: json['backgroundImageUrl']?.toString(),
      avatarImageUrl: json['avatarImageUrl']?.toString(),
      addressLine: json['addressLine']?.toString() ?? '',
      latitude: _toDouble(json['latitude']),
      longitude: _toDouble(json['longitude']),
      avgRating: _toDouble(json['avgRating']),
      reviewCount: _toInt(json['reviewCount']),
      open: json['open'] == true,
      maxDeliveryKm: _toDouble(json['maxDeliveryKm']),
    );
  }

  static int _toInt(Object? value) {
    if (value is int) {
      return value;
    }

    return int.tryParse(value?.toString() ?? '') ?? 0;
  }

  static double _toDouble(Object? value) {
    if (value is num) {
      return value.toDouble();
    }

    return double.tryParse(value?.toString() ?? '') ?? 0;
  }
}
