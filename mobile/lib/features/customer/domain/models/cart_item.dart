class CartItem {
  const CartItem({
    required this.menuItemId,
    required this.menuItemName,
    required this.unitPrice,
    required this.quantity,
    required this.lineTotal,
    required this.note,
  });

  final String menuItemId;
  final String menuItemName;
  final double unitPrice;
  final int quantity;
  final double lineTotal;
  final String? note;

  factory CartItem.fromJson(Map<String, dynamic> json) {
    return CartItem(
      menuItemId: json['menuItemId']?.toString() ?? '',
      menuItemName: json['menuItemName']?.toString() ?? '',
      unitPrice: _toDouble(json['unitPrice']),
      quantity: _toInt(json['quantity']),
      lineTotal: _toDouble(json['lineTotal']),
      note: json['note']?.toString(),
    );
  }

  static double _toDouble(Object? value) {
    if (value is num) {
      return value.toDouble();
    }
    return double.tryParse(value?.toString() ?? '') ?? 0;
  }

  static int _toInt(Object? value) {
    if (value is int) {
      return value;
    }
    return int.tryParse(value?.toString() ?? '') ?? 0;
  }
}
