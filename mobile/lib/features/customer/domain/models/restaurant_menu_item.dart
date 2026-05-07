class RestaurantMenuItem {
  const RestaurantMenuItem({
    required this.id,
    required this.restaurantId,
    required this.categoryId,
    required this.taxonomyCodes,
    required this.name,
    required this.description,
    required this.imageUrl,
    required this.price,
    required this.active,
    required this.available,
  });

  final String id;
  final String restaurantId;
  final String categoryId;
  final List<String> taxonomyCodes;
  final String name;
  final String? description;
  final String? imageUrl;
  final double price;
  final bool active;
  final bool available;

  factory RestaurantMenuItem.fromJson(Map<String, dynamic> json) {
    return RestaurantMenuItem(
      id: json['id']?.toString() ?? '',
      restaurantId: json['restaurantId']?.toString() ?? '',
      categoryId: json['categoryId']?.toString() ?? '',
      taxonomyCodes: _toStrings(json['taxonomyCodes']),
      name: json['name']?.toString() ?? '',
      description: json['description']?.toString(),
      imageUrl: json['imageUrl']?.toString(),
      price: _toDouble(json['price']),
      active: json['active'] == true,
      available: json['available'] == true,
    );
  }

  static List<String> _toStrings(Object? raw) {
    if (raw is! List) {
      return const [];
    }

    return raw.map((item) => item.toString()).toList(growable: false);
  }

  static double _toDouble(Object? value) {
    if (value is num) {
      return value.toDouble();
    }

    return double.tryParse(value?.toString() ?? '') ?? 0;
  }
}
