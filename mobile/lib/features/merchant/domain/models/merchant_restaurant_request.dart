class MerchantRestaurantRequest {
  const MerchantRestaurantRequest({
    required this.name,
    required this.cuisineType,
    required this.cuisineTypes,
    required this.description,
    required this.addressLine,
    required this.latitude,
    required this.longitude,
    required this.maxDeliveryKm,
    required this.isOpen,
  });

  final String name;
  final String cuisineType;
  final List<String> cuisineTypes;
  final String description;
  final String addressLine;
  final double latitude;
  final double longitude;
  final double maxDeliveryKm;
  final bool isOpen;

  Map<String, dynamic> toJson() {
    return {
      'name': name.trim(),
      'cuisineType': cuisineType.trim(),
      'cuisineTypes': cuisineTypes
          .map((item) => item.trim())
          .where((item) => item.isNotEmpty)
          .toList(growable: false),
      'description': description.trim(),
      'addressLine': addressLine.trim(),
      'latitude': latitude,
      'longitude': longitude,
      'maxDeliveryKm': maxDeliveryKm,
      'isOpen': isOpen,
    };
  }
}
