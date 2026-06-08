class MerchantMenuCategory {
  const MerchantMenuCategory({
    required this.id,
    required this.restaurantId,
    required this.name,
    required this.sortOrder,
    required this.active,
  });

  final String id;
  final String restaurantId;
  final String name;
  final int sortOrder;
  final bool active;

  factory MerchantMenuCategory.fromJson(Map<String, dynamic> json) {
    return MerchantMenuCategory(
      id: json['id']?.toString() ?? '',
      restaurantId: json['restaurantId']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      sortOrder: _toInt(json['sortOrder']),
      active: json['active'] == true,
    );
  }

  static int _toInt(Object? value) {
    if (value is int) {
      return value;
    }
    return int.tryParse(value?.toString() ?? '') ?? 0;
  }
}
