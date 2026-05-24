import '../models/user_profile.dart';

abstract class CustomerProfileRepository {
  Future<UserProfile> me();

  Future<UserProfile> updateProfile({
    required String fullName,
    required String email,
    required String phoneNumber,
    String? avatarUrl,
  });

  Future<void> changePassword({
    required String currentPassword,
    required String newPassword,
    required String confirmPassword,
  });
}
