import '../../../../core/auth/auth_session_recovery.dart';
import '../../domain/models/merchant_notification.dart';
import '../../domain/repositories/merchant_notification_repository.dart';
import '../data_sources/merchant_notification_remote_data_source.dart';

class HttpMerchantNotificationRepository
    implements MerchantNotificationRepository {
  HttpMerchantNotificationRepository({
    required MerchantNotificationRemoteDataSource remoteDataSource,
    required AuthSessionRecovery sessionRecovery,
  }) : _remoteDataSource = remoteDataSource,
       _sessionRecovery = sessionRecovery;

  final MerchantNotificationRemoteDataSource _remoteDataSource;
  final AuthSessionRecovery _sessionRecovery;

  @override
  Future<List<MerchantNotification>> listNotifications({
    int page = 0,
    int size = 20,
  }) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.list(
        accessToken: accessToken,
        page: page,
        size: size,
      );
    });
  }

  @override
  Future<MerchantNotification> markAsRead(String id) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.markAsRead(accessToken: accessToken, id: id);
    });
  }
}
