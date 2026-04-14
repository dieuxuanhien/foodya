import 'package:equatable/equatable.dart';

import 'user_role.dart';

class SessionState extends Equatable {
  const SessionState._({required this.isAuthenticated, required this.role});

  const SessionState.unauthenticated()
    : this._(isAuthenticated: false, role: null);

  const SessionState.authenticated(UserRole userRole)
    : this._(isAuthenticated: true, role: userRole);

  final bool isAuthenticated;
  final UserRole? role;

  @override
  List<Object?> get props => [isAuthenticated, role];
}
