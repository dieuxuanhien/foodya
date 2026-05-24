import '../../../../core/auth/auth_session_recovery.dart';
import '../../domain/models/customer_notification.dart';
import '../../domain/repositories/customer_notification_repository.dart';
import '../data_sources/customer_notification_remote_data_source.dart';

class HttpCustomerNotificationRepository
    implements CustomerNotificationRepository {
  HttpCustomerNotificationRepository({
    required CustomerNotificationRemoteDataSource remoteDataSource,
    required AuthSessionRecovery sessionRecovery,
  }) : _remoteDataSource = remoteDataSource,
       _sessionRecovery = sessionRecovery;

  final CustomerNotificationRemoteDataSource _remoteDataSource;
  final AuthSessionRecovery _sessionRecovery;

  @override
  Future<List<CustomerNotification>> listNotifications({
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
  Future<CustomerNotification> markAsRead(String id) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.markAsRead(accessToken: accessToken, id: id);
    });
  }
}
