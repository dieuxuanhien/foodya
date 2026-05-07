class RestaurantSearchItem {
  const RestaurantSearchItem({
    required this.restaurantId,
    required this.restaurantName,
    required this.cuisine,
    required this.backgroundImageUrl,
    required this.avatarImageUrl,
    required this.rating,
    required this.openStatus,
    required this.maxDeliveryKm,
    required this.distanceKm,
    required this.matchedItems,
  });

  final String restaurantId;
  final String restaurantName;
  final String cuisine;
  final String? backgroundImageUrl;
  final String? avatarImageUrl;
  final double rating;
  final bool openStatus;
  final double maxDeliveryKm;
  final double? distanceKm;
  final List<MatchedMenuItem> matchedItems;

  factory RestaurantSearchItem.fromJson(Map<String, dynamic> json) {
    return RestaurantSearchItem(
      restaurantId: json['restaurantId']?.toString() ?? '',
      restaurantName: json['restaurantName']?.toString() ?? '',
      cuisine: json['cuisine']?.toString() ?? '',
      backgroundImageUrl: json['backgroundImageUrl']?.toString(),
      avatarImageUrl: json['avatarImageUrl']?.toString(),
      rating: _toDouble(json['rating']),
      openStatus: json['openStatus'] == true,
      maxDeliveryKm: _toDouble(json['maxDeliveryKm']),
      distanceKm: _toNullableDouble(json['distanceKm']),
      matchedItems: _toMatchedItems(json['matchedItems']),
    );
  }

  static List<MatchedMenuItem> _toMatchedItems(Object? raw) {
    if (raw is! List) {
      return const [];
    }

    return raw
        .whereType<Map>()
        .map((item) {
          final map = item.map((key, value) => MapEntry(key.toString(), value));
          return MatchedMenuItem.fromJson(map);
        })
        .toList(growable: false);
  }

  static double _toDouble(Object? value) {
    if (value is num) {
      return value.toDouble();
    }

    return double.tryParse(value?.toString() ?? '') ?? 0;
  }

  static double? _toNullableDouble(Object? value) {
    if (value == null) {
      return null;
    }

    if (value is num) {
      return value.toDouble();
    }

    return double.tryParse(value.toString());
  }
}

class MatchedMenuItem {
  const MatchedMenuItem({
    required this.id,
    required this.name,
    required this.price,
    required this.imageUrl,
  });

  final String id;
  final String name;
  final double price;
  final String? imageUrl;

  factory MatchedMenuItem.fromJson(Map<String, dynamic> json) {
    return MatchedMenuItem(
      id: json['id']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      price: _toDouble(json['price']),
      imageUrl: json['imageUrl']?.toString(),
    );
  }

  static double _toDouble(Object? value) {
    if (value is num) {
      return value.toDouble();
    }

    return double.tryParse(value?.toString() ?? '') ?? 0;
  }
}
