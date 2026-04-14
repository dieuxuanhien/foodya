import '../../../../core/auth/user_role.dart';

abstract class AuthRepository {
  Future<void> loginAs(UserRole role);

  Future<void> logout();
}
