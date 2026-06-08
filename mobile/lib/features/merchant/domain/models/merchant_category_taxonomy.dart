class MerchantCategoryTaxonomy {
  const MerchantCategoryTaxonomy({
    required this.code,
    required this.displayName,
    required this.sortOrder,
    required this.active,
  });

  final String code;
  final String displayName;
  final int sortOrder;
  final bool active;

  factory MerchantCategoryTaxonomy.fromJson(Map<String, dynamic> json) {
    return MerchantCategoryTaxonomy(
      code: json['code']?.toString() ?? '',
      displayName: json['displayName']?.toString() ?? '',
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
