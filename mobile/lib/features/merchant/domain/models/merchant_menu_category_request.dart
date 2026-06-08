class MerchantMenuCategoryRequest {
  const MerchantMenuCategoryRequest({
    required this.name,
    required this.sortOrder,
    required this.isActive,
  });

  final String name;
  final int sortOrder;
  final bool isActive;

  Map<String, dynamic> toJson() {
    return {'name': name, 'sortOrder': sortOrder, 'isActive': isActive};
  }
}
