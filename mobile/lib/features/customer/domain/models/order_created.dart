class OrderCreated {
  const OrderCreated({
    required this.orderId,
    required this.orderCode,
    required this.status,
    required this.paymentMethod,
    required this.paymentStatus,
    required this.subtotalAmount,
    required this.deliveryFee,
    required this.totalAmount,
    required this.commissionAmount,
    required this.shippingFeeMarginAmount,
    required this.platformProfitAmount,
    required this.currencyCode,
  });

  final String orderId;
  final String orderCode;
  final String status;
  final String paymentMethod;
  final String paymentStatus;
  final double subtotalAmount;
  final double deliveryFee;
  final double totalAmount;
  final double commissionAmount;
  final double shippingFeeMarginAmount;
  final double platformProfitAmount;
  final String currencyCode;

  factory OrderCreated.fromJson(Map<String, dynamic> json) {
    return OrderCreated(
      orderId: json['orderId']?.toString() ?? '',
      orderCode: json['orderCode']?.toString() ?? '',
      status: json['status']?.toString() ?? '',
      paymentMethod: json['paymentMethod']?.toString() ?? '',
      paymentStatus: json['paymentStatus']?.toString() ?? '',
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
