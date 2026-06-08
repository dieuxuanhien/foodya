import '../../../../core/auth/auth_session_recovery.dart';
import '../../domain/models/merchant_revenue_report.dart';
import '../../domain/repositories/merchant_revenue_repository.dart';
import '../data_sources/merchant_revenue_remote_data_source.dart';

class HttpMerchantRevenueRepository implements MerchantRevenueRepository {
  HttpMerchantRevenueRepository({
    required MerchantRevenueRemoteDataSource remoteDataSource,
    required AuthSessionRecovery sessionRecovery,
  }) : _remoteDataSource = remoteDataSource,
       _sessionRecovery = sessionRecovery;

  final MerchantRevenueRemoteDataSource _remoteDataSource;
  final AuthSessionRecovery _sessionRecovery;

  @override
  Future<MerchantRevenueReport> getRevenueReport({
    DateTime? from,
    DateTime? to,
    int topItems = 5,
  }) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.getRevenueReport(
        accessToken: accessToken,
        from: from,
        to: to,
        topItems: topItems,
      );
    });
  }
}
