import '../../../../core/auth/user_role.dart';

class RegisterRequest {
  const RegisterRequest({
    required this.username,
    required this.email,
    required this.phoneNumber,
    required this.fullName,
    required this.password,
    required this.role,
  });

  final String username;
  final String email;
  final String phoneNumber;
  final String fullName;
  final String password;
  final UserRole role;

  Map<String, dynamic> toJson() {
    return {
      'username': username,
      'email': email,
      'phoneNumber': phoneNumber,
      'fullName': fullName,
      'password': password,
      'role': role.apiValue,
    };
  }
}
