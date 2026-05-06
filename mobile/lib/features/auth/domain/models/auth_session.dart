import 'package:equatable/equatable.dart';

import '../../../../core/auth/auth_tokens.dart';
import '../../../../core/auth/user_role.dart';

class AuthSession extends Equatable {
  const AuthSession({required this.role, required this.tokens});

  final UserRole role;
  final AuthTokens tokens;

  @override
  List<Object?> get props => [role, tokens];
}
