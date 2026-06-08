class MerchantOrderSummary {
  const MerchantOrderSummary({
    required this.orderId,
    required this.orderCode,
    required this.customerName,
    required this.restaurantName,
    required this.status,
    required this.paymentStatus,
    required this.totalAmount,
  });

  final String orderId;
  final String orderCode;
  final String customerName;
  final String restaurantName;
  final String status;
  final String paymentStatus;
  final double totalAmount;

  factory MerchantOrderSummary.fromJson(Map<String, dynamic> json) {
    return MerchantOrderSummary(
      orderId: json['orderId']?.toString() ?? '',
      orderCode: json['orderCode']?.toString() ?? '',
      customerName: json['customerName']?.toString() ?? '',
      restaurantName: json['restaurantName']?.toString() ?? '',
      status: json['status']?.toString() ?? '',
      paymentStatus: json['paymentStatus']?.toString() ?? '',
      totalAmount: _toDouble(json['totalAmount']),
    );
  }

  static double _toDouble(Object? value) {
    if (value is num) {
      return value.toDouble();
    }
    return double.tryParse(value?.toString() ?? '') ?? 0;
  }
}
