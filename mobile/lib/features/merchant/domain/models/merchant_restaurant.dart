class MerchantRestaurant {
  const MerchantRestaurant({
    required this.id,
    required this.name,
    required this.cuisineType,
    required this.description,
    this.backgroundImageUrl,
    this.avatarImageUrl,
    required this.addressLine,
    required this.latitude,
    required this.longitude,
    required this.avgRating,
    required this.reviewCount,
    required this.status,
    required this.open,
    required this.maxDeliveryKm,
  });

  final String id;
  final String name;
  final String cuisineType;
  final String description;
  final String? backgroundImageUrl;
  final String? avatarImageUrl;
  final String addressLine;
  final double latitude;
  final double longitude;
  final double avgRating;
  final int reviewCount;
  final String status;
  final bool open;
  final double maxDeliveryKm;

  factory MerchantRestaurant.fromJson(Map<String, dynamic> json) {
    return MerchantRestaurant(
      id: json['id']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      cuisineType: json['cuisineType']?.toString() ?? '',
      description: json['description']?.toString() ?? '',
      backgroundImageUrl: _nullableText(json['backgroundImageUrl']),
      avatarImageUrl: _nullableText(json['avatarImageUrl']),
      addressLine: json['addressLine']?.toString() ?? '',
      latitude: _toDouble(json['latitude']),
      longitude: _toDouble(json['longitude']),
      avgRating: _toDouble(json['avgRating']),
      reviewCount: _toInt(json['reviewCount']),
      status: json['status']?.toString() ?? '',
      open: json['open'] == true,
      maxDeliveryKm: _toDouble(json['maxDeliveryKm']),
    );
  }

  static String? _nullableText(Object? value) {
    final text = value?.toString().trim();
    return text == null || text.isEmpty ? null : text;
  }

  static double _toDouble(Object? value) {
    if (value is num) {
      return value.toDouble();
    }
    return double.tryParse(value?.toString() ?? '') ?? 0.0;
  }

  static int _toInt(Object? value) {
    if (value is int) {
      return value;
    }
    return int.tryParse(value?.toString() ?? '') ?? 0;
  }
}
