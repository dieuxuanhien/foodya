class MerchantReview {
  const MerchantReview({
    required this.reviewId,
    required this.orderId,
    required this.restaurantId,
    required this.customerUserId,
    required this.stars,
    required this.comment,
    this.merchantResponse,
    this.respondedAt,
    required this.createdAt,
  });

  final String reviewId;
  final String orderId;
  final String restaurantId;
  final String customerUserId;
  final int stars;
  final String comment;
  final String? merchantResponse;
  final DateTime? respondedAt;
  final DateTime createdAt;

  factory MerchantReview.fromJson(Map<String, dynamic> json) {
    return MerchantReview(
      reviewId: json['reviewId']?.toString() ?? '',
      orderId: json['orderId']?.toString() ?? '',
      restaurantId: json['restaurantId']?.toString() ?? '',
      customerUserId: json['customerUserId']?.toString() ?? '',
      stars: _toInt(json['stars']),
      comment: json['comment']?.toString() ?? '',
      merchantResponse: _nullableText(json['merchantResponse']),
      respondedAt: _toDate(json['respondedAt']),
      createdAt:
          _toDate(json['createdAt']) ?? DateTime.fromMillisecondsSinceEpoch(0),
    );
  }

  static String? _nullableText(Object? value) {
    final text = value?.toString().trim();
    return text == null || text.isEmpty ? null : text;
  }

  static DateTime? _toDate(Object? value) {
    if (value == null) {
      return null;
    }
    return DateTime.tryParse(value.toString());
  }

  static int _toInt(Object? value) {
    if (value is int) {
      return value;
    }
    return int.tryParse(value?.toString() ?? '') ?? 0;
  }
}
