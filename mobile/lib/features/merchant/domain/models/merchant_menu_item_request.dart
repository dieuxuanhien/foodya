class MerchantMenuItemRequest {
  const MerchantMenuItemRequest({
    required this.categoryId,
    required this.taxonomyCodes,
    required this.name,
    required this.description,
    required this.price,
    required this.isActive,
    required this.isAvailable,
  });

  final String categoryId;
  final List<String> taxonomyCodes;
  final String name;
  final String description;
  final double price;
  final bool isActive;
  final bool isAvailable;

  Map<String, dynamic> toJson() {
    return {
      'categoryId': categoryId,
      'taxonomyCodes': taxonomyCodes,
      'name': name,
      'description': description,
      'price': price,
      'isActive': isActive,
      'isAvailable': isAvailable,
    };
  }
}
