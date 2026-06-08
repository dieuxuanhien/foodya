class MerchantRevenueReport {
  const MerchantRevenueReport({
    required this.fromDate,
    required this.toDate,
    required this.revenue,
    required this.platformProfit,
    required this.orderCount,
    required this.avgOrderValue,
    required this.series,
    required this.topSellingItems,
  });

  final DateTime fromDate;
  final DateTime toDate;
  final double revenue;
  final double platformProfit;
  final int orderCount;
  final double avgOrderValue;
  final List<MerchantRevenueBucket> series;
  final List<MerchantTopSellingItem> topSellingItems;

  factory MerchantRevenueReport.fromJson(Map<String, dynamic> json) {
    return MerchantRevenueReport(
      fromDate:
          DateTime.tryParse(json['fromDate']?.toString() ?? '') ??
          DateTime.fromMillisecondsSinceEpoch(0),
      toDate:
          DateTime.tryParse(json['toDate']?.toString() ?? '') ??
          DateTime.fromMillisecondsSinceEpoch(0),
      revenue: _toDouble(json['revenue']),
      platformProfit: _toDouble(json['platformProfit']),
      orderCount: _toInt(json['orderCount']),
      avgOrderValue: _toDouble(json['avgOrderValue']),
      series: _toMaps(
        json['series'],
      ).map(MerchantRevenueBucket.fromJson).toList(growable: false),
      topSellingItems: _toMaps(
        json['topSellingItems'],
      ).map(MerchantTopSellingItem.fromJson).toList(growable: false),
    );
  }

  static List<Map<String, dynamic>> _toMaps(Object? raw) {
    if (raw is! List) {
      return const [];
    }
    return raw
        .whereType<Map>()
        .map(
          (item) => item.map((key, value) => MapEntry(key.toString(), value)),
        )
        .toList(growable: false);
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

class MerchantRevenueBucket {
  const MerchantRevenueBucket({
    required this.period,
    required this.revenue,
    required this.platformProfit,
    required this.orderCount,
    required this.avgOrderValue,
  });

  final DateTime period;
  final double revenue;
  final double platformProfit;
  final int orderCount;
  final double avgOrderValue;

  factory MerchantRevenueBucket.fromJson(Map<String, dynamic> json) {
    return MerchantRevenueBucket(
      period:
          DateTime.tryParse(json['period']?.toString() ?? '') ??
          DateTime.fromMillisecondsSinceEpoch(0),
      revenue: MerchantRevenueReport._toDouble(json['revenue']),
      platformProfit: MerchantRevenueReport._toDouble(json['platformProfit']),
      orderCount: MerchantRevenueReport._toInt(json['orderCount']),
      avgOrderValue: MerchantRevenueReport._toDouble(json['avgOrderValue']),
    );
  }
}

class MerchantTopSellingItem {
  const MerchantTopSellingItem({
    required this.menuItemId,
    required this.itemName,
    required this.quantitySold,
    required this.revenue,
  });

  final String menuItemId;
  final String itemName;
  final int quantitySold;
  final double revenue;

  factory MerchantTopSellingItem.fromJson(Map<String, dynamic> json) {
    return MerchantTopSellingItem(
      menuItemId: json['menuItemId']?.toString() ?? '',
      itemName: json['itemName']?.toString() ?? '',
      quantitySold: MerchantRevenueReport._toInt(json['quantitySold']),
      revenue: MerchantRevenueReport._toDouble(json['revenue']),
    );
  }
}
