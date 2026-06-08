class OrderCostReview {
  const OrderCostReview({
    required this.subtotalAmount,
    required this.deliveryFee,
    required this.totalAmount,
    required this.commissionAmount,
    required this.shippingFeeMarginAmount,
    required this.platformProfitAmount,
    required this.currencyCode,
  });

  final double subtotalAmount;
  final double deliveryFee;
  final double totalAmount;
  final double commissionAmount;
  final double shippingFeeMarginAmount;
  final double platformProfitAmount;
  final String currencyCode;

  factory OrderCostReview.fromJson(Map<String, dynamic> json) {
    return OrderCostReview(
      subtotalAmount: _toDouble(json['subtotalAmount']),
      deliveryFee: _toDouble(json['deliveryFee']),
      totalAmount: _toDouble(json['totalAmount']),
      commissionAmount: _toDouble(json['commissionAmount']),
      shippingFeeMarginAmount: _toDouble(json['shippingFeeMarginAmount']),
      platformProfitAmount: _toDouble(json['platformProfitAmount']),
      currencyCode: json['currencyCode']?.toString() ?? 'VND',
    );
  }

  static double _toDouble(Object? value) {
    if (value is num) {
      return value.toDouble();
    }
    return double.tryParse(value?.toString() ?? '') ?? 0;
  }
}
