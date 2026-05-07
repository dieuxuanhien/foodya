class CategoryTaxonomy {
  const CategoryTaxonomy({
    required this.code,
    required this.displayName,
    required this.sortOrder,
    required this.isActive,
  });

  final String code;
  final String displayName;
  final int sortOrder;
  final bool isActive;

  factory CategoryTaxonomy.fromJson(Map<String, dynamic> json) {
    return CategoryTaxonomy(
      code: json['code']?.toString() ?? '',
      displayName: json['displayName']?.toString() ?? '',
      sortOrder: _toInt(json['sortOrder']),
      isActive: json['active'] == true,
    );
  }

  static int _toInt(Object? value) {
    if (value is int) {
      return value;
    }

    return int.tryParse(value?.toString() ?? '') ?? 0;
  }
}
