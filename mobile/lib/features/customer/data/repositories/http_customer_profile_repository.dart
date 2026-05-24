import '../../../../core/auth/auth_session_recovery.dart';
import '../../domain/models/user_profile.dart';
import '../../domain/repositories/customer_profile_repository.dart';
import '../data_sources/customer_profile_remote_data_source.dart';

class HttpCustomerProfileRepository implements CustomerProfileRepository {
  HttpCustomerProfileRepository({
    required CustomerProfileRemoteDataSource remoteDataSource,
    required AuthSessionRecovery sessionRecovery,
  }) : _remoteDataSource = remoteDataSource,
       _sessionRecovery = sessionRecovery;

  final CustomerProfileRemoteDataSource _remoteDataSource;
  final AuthSessionRecovery _sessionRecovery;

  @override
  Future<UserProfile> me() {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.me(accessToken: accessToken);
    });
  }

  @override
  Future<UserProfile> updateProfile({
    required String fullName,
    required String email,
    required String phoneNumber,
    String? avatarUrl,
  }) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.updateProfile(
        accessToken: accessToken,
        fullName: fullName,
        email: email,
        phoneNumber: phoneNumber,
        avatarUrl: avatarUrl,
      );
    });
  }

  @override
  Future<void> changePassword({
    required String currentPassword,
    required String newPassword,
    required String confirmPassword,
  }) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.changePassword(
        accessToken: accessToken,
        currentPassword: currentPassword,
        newPassword: newPassword,
        confirmPassword: confirmPassword,
      );
    });
  }
}
