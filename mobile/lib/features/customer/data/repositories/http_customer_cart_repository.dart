import '../../../../core/auth/auth_session_recovery.dart';
import '../../domain/models/active_cart.dart';
import '../../domain/repositories/customer_cart_repository.dart';
import '../data_sources/customer_cart_remote_data_source.dart';

class HttpCustomerCartRepository implements CustomerCartRepository {
  HttpCustomerCartRepository({
    required CustomerCartRemoteDataSource remoteDataSource,
    required AuthSessionRecovery sessionRecovery,
  }) : _remoteDataSource = remoteDataSource,
       _sessionRecovery = sessionRecovery;

  final CustomerCartRemoteDataSource _remoteDataSource;
  final AuthSessionRecovery _sessionRecovery;

  @override
  Future<ActiveCart> getActiveCart() {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.getActiveCart(accessToken: accessToken);
    });
  }

  @override
  Future<ActiveCart> addItem({
    required String menuItemId,
    required int quantity,
    String? note,
  }) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.addItem(
        accessToken: accessToken,
        menuItemId: menuItemId,
        quantity: quantity,
        note: note,
      );
    });
  }

  @override
  Future<ActiveCart> updateItem({
    required String menuItemId,
    required int quantity,
    String? note,
  }) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.updateItem(
        accessToken: accessToken,
        menuItemId: menuItemId,
        quantity: quantity,
        note: note,
      );
    });
  }

  @override
  Future<ActiveCart> removeItem(String menuItemId) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.removeItem(
        accessToken: accessToken,
        menuItemId: menuItemId,
      );
    });
  }

  @override
  Future<ActiveCart> clearCart() {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.clearCart(accessToken: accessToken);
    });
  }
}
