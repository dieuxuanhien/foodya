class MerchantNotification {
  const MerchantNotification({
    required this.id,
    required this.receiverUserId,
    required this.receiverType,
    required this.eventType,
    required this.title,
    required this.message,
    required this.status,
    this.orderId,
    this.sentAt,
    this.readAt,
    required this.createdAt,
  });

  final String id;
  final String receiverUserId;
  final String receiverType;
  final String eventType;
  final String title;
  final String message;
  final String status;
  final String? orderId;
  final DateTime? sentAt;
  final DateTime? readAt;
  final DateTime createdAt;

  bool get isRead => readAt != null || status.toUpperCase() == 'READ';
  bool get isOrderNotification =>
      orderId != null || eventType.toUpperCase().contains('ORDER');

  factory MerchantNotification.fromJson(Map<String, dynamic> json) {
    return MerchantNotification(
      id: json['id']?.toString() ?? '',
      receiverUserId: json['receiverUserId']?.toString() ?? '',
      receiverType: json['receiverType']?.toString() ?? '',
      eventType: json['eventType']?.toString() ?? '',
      title: json['title']?.toString() ?? '',
      message: json['message']?.toString() ?? '',
      status: json['status']?.toString() ?? '',
      orderId: _nullableText(json['orderId']),
      sentAt: _toDate(json['sentAt']),
      readAt: _toDate(json['readAt']),
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
}
