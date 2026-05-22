import 'cart_item.dart';

class ActiveCart {
  const ActiveCart({
    required this.cartId,
    required this.restaurantId,
    required this.restaurantName,
    required this.subtotal,
    required this.itemCount,
    required this.items,
  });

  final String cartId;
  final String restaurantId;
  final String restaurantName;
  final double subtotal;
  final int itemCount;
  final List<CartItem> items;

  bool get isEmpty => items.isEmpty;

  factory ActiveCart.fromJson(Map<String, dynamic> json) {
    final items =
        (json['items'] is List)
            ? (json['items'] as List)
                .whereType<Map>()
                .map(
                  (item) => item.map(
                    (key, value) => MapEntry(key.toString(), value),
                  ),
                )
                .map(CartItem.fromJson)
                .toList(growable: false)
            : const <CartItem>[];

    return ActiveCart(
      cartId: json['cartId']?.toString() ?? '',
      restaurantId: json['restaurantId']?.toString() ?? '',
      restaurantName: json['restaurantName']?.toString() ?? '',
      subtotal: _toDouble(json['subtotal']),
      itemCount: _toInt(json['itemCount']),
      items: items,
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
