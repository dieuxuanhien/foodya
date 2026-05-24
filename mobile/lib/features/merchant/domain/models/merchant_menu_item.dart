class MerchantMenuItem {
  const MerchantMenuItem({
    required this.id,
    required this.restaurantId,
    required this.restaurantName,
    required this.categoryId,
    required this.categoryName,
    required this.taxonomyCodes,
    required this.name,
    required this.description,
    this.imageUrl,
    required this.price,
    required this.active,
    required this.available,
  });

  final String id;
  final String restaurantId;
  final String restaurantName;
  final String categoryId;
  final String categoryName;
  final List<String> taxonomyCodes;
  final String name;
  final String description;
  final String? imageUrl;
  final double price;
  final bool active;
  final bool available;

  factory MerchantMenuItem.fromJson(Map<String, dynamic> json) {
    return MerchantMenuItem(
      id: json['id']?.toString() ?? '',
      restaurantId: json['restaurantId']?.toString() ?? '',
      restaurantName: json['restaurantName']?.toString() ?? '',
      categoryId: json['categoryId']?.toString() ?? '',
      categoryName: json['categoryName']?.toString() ?? '',
      taxonomyCodes: _toStrings(json['taxonomyCodes']),
      name: json['name']?.toString() ?? '',
      description: json['description']?.toString() ?? '',
      imageUrl: _nullableText(json['imageUrl']),
      price: _toDouble(json['price']),
      active: json['active'] == true,
      available: json['available'] == true,
    );
  }

  static String? _nullableText(Object? value) {
    final text = value?.toString().trim();
    return text == null || text.isEmpty ? null : text;
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
