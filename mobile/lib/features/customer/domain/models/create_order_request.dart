class CreateOrderRequest {
  const CreateOrderRequest({
    required this.deliveryAddress,
    required this.deliveryLatitude,
    required this.deliveryLongitude,
    this.customerNote,
  });

  final String deliveryAddress;
  final double deliveryLatitude;
  final double deliveryLongitude;
  final String? customerNote;

  Map<String, dynamic> toJson() {
    return {
      'deliveryAddress': deliveryAddress,
      'deliveryLatitude': deliveryLatitude,
      'deliveryLongitude': deliveryLongitude,
      if (customerNote != null && customerNote!.trim().isNotEmpty)
        'customerNote': customerNote!.trim(),
    };
  }
}
