import 'dart:async';

import '../../../../core/auth/user_role.dart';
import '../../domain/repositories/auth_repository.dart';

class MockAuthRepository implements AuthRepository {
  @override
  Future<void> loginAs(UserRole role) async {
    await Future<void>.delayed(const Duration(milliseconds: 300));
  }

  @override
  Future<void> logout() async {
    await Future<void>.delayed(const Duration(milliseconds: 100));
  }
}
