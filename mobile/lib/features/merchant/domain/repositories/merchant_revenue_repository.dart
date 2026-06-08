import '../models/merchant_revenue_report.dart';

abstract class MerchantRevenueRepository {
  Future<MerchantRevenueReport> getRevenueReport({
    DateTime? from,
    DateTime? to,
    int topItems = 5,
  });
}
