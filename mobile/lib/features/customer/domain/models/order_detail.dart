class OrderDetail {
  const OrderDetail({
    required this.orderId,
    required this.orderCode,
    required this.restaurantId,
    required this.restaurantName,
    required this.customerUserId,
    required this.customerName,
    required this.status,
    required this.paymentMethod,
    required this.paymentStatus,
    required this.subtotalAmount,
    required this.deliveryFee,
    required this.totalAmount,
    required this.deliveryAddress,
    this.deliveryLatitude,
    this.deliveryLongitude,
  });

  final String orderId;
  final String orderCode;
  final String restaurantId;
  final String restaurantName;
  final String customerUserId;
  final String customerName;
  final String status;
  final String paymentMethod;
  final String paymentStatus;
  final double subtotalAmount;
  final double deliveryFee;
  final double totalAmount;
  final String deliveryAddress;
  final double? deliveryLatitude;
  final double? deliveryLongitude;

  factory OrderDetail.fromJson(Map<String, dynamic> json) {
    return OrderDetail(
      orderId: json['orderId']?.toString() ?? '',
      orderCode: json['orderCode']?.toString() ?? '',
      restaurantId: json['restaurantId']?.toString() ?? '',
      restaurantName: json['restaurantName']?.toString() ?? '',
      customerUserId: json['customerUserId']?.toString() ?? '',
      customerName: json['customerName']?.toString() ?? '',
      status: json['status']?.toString() ?? '',
      paymentMethod: json['paymentMethod']?.toString() ?? '',
      paymentStatus: json['paymentStatus']?.toString() ?? '',
      subtotalAmount: _toDouble(json['subtotalAmount']),
      deliveryFee: _toDouble(json['deliveryFee']),
      totalAmount: _toDouble(json['totalAmount']),
      deliveryAddress: json['deliveryAddress']?.toString() ?? '',
      deliveryLatitude: _toNullableDouble(json['deliveryLatitude']),
      deliveryLongitude: _toNullableDouble(json['deliveryLongitude']),
    );
  }

  static double _toDouble(Object? value) {
    if (value is num) {
      return value.toDouble();
    }
    return double.tryParse(value?.toString() ?? '') ?? 0;
  }

  static double? _toNullableDouble(Object? value) {
    if (value == null) {
      return null;
    }
    if (value is num) {
      return value.toDouble();
    }
    return double.tryParse(value.toString());
  }
}
