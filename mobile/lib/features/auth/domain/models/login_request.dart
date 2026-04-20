class LoginRequest {
  const LoginRequest({required this.usernameOrEmail, required this.password});

  final String usernameOrEmail;
  final String password;

  Map<String, dynamic> toJson() {
    return {'usernameOrEmail': usernameOrEmail, 'password': password};
  }
}
