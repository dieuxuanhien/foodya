class AiRecommendationItem {
  const AiRecommendationItem({
    required this.menuItemId,
    required this.menuItemName,
    required this.restaurantId,
    required this.restaurantName,
    required this.price,
    this.distanceKm,
    this.restaurantRating,
    required this.reason,
  });

  final String menuItemId;
  final String menuItemName;
  final String restaurantId;
  final String restaurantName;
  final double price;
  final double? distanceKm;
  final double? restaurantRating;
  final String reason;

  factory AiRecommendationItem.fromJson(Map<String, dynamic> json) {
    return AiRecommendationItem(
      menuItemId: json['menuItemId']?.toString() ?? '',
      menuItemName: json['menuItemName']?.toString() ?? '',
      restaurantId: json['restaurantId']?.toString() ?? '',
      restaurantName: json['restaurantName']?.toString() ?? '',
      price: _toDouble(json['price']),
      distanceKm: _toNullableDouble(json['distanceKm']),
      restaurantRating: _toNullableDouble(json['restaurantRating']),
      reason: json['reason']?.toString() ?? '',
    );
  }

  static double _toDouble(Object? value) {
    if (value is num) {
      return value.toDouble();
    }
    return double.tryParse(value?.toString() ?? '') ?? 0.0;
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

class AiChatResponse {
  const AiChatResponse({
    required this.chatId,
    required this.prompt,
    required this.responseSummary,
    required this.recommendations,
    required this.createdAt,
  });

  final String chatId;
  final String prompt;
  final String responseSummary;
  final List<AiRecommendationItem> recommendations;
  final DateTime createdAt;

  factory AiChatResponse.fromJson(Map<String, dynamic> json) {
    final rawItems = json['recommendations'];
    final items =
        rawItems is List
            ? rawItems
                .whereType<Map>()
                .map(
                  (item) => AiRecommendationItem.fromJson(
                    item.map((key, value) => MapEntry(key.toString(), value)),
                  ),
                )
                .toList(growable: false)
            : const <AiRecommendationItem>[];

    return AiChatResponse(
      chatId: json['chatId']?.toString() ?? '',
      prompt: json['prompt']?.toString() ?? '',
      responseSummary: json['responseSummary']?.toString() ?? '',
      recommendations: items,
      createdAt:
          DateTime.tryParse(json['createdAt']?.toString() ?? '') ??
          DateTime.fromMillisecondsSinceEpoch(0),
    );
  }
}

class AiChatHistoryItem {
  const AiChatHistoryItem({
    required this.chatId,
    required this.prompt,
    required this.responseSummary,
    required this.createdAt,
  });

  final String chatId;
  final String prompt;
  final String responseSummary;
  final DateTime createdAt;

  factory AiChatHistoryItem.fromJson(Map<String, dynamic> json) {
    return AiChatHistoryItem(
      chatId: json['chatId']?.toString() ?? '',
      prompt: json['prompt']?.toString() ?? '',
      responseSummary: json['responseSummary']?.toString() ?? '',
      createdAt:
          DateTime.tryParse(json['createdAt']?.toString() ?? '') ??
          DateTime.fromMillisecondsSinceEpoch(0),
    );
  }
}
