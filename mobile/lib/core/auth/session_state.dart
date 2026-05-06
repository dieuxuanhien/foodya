import 'package:equatable/equatable.dart';

import 'user_role.dart';

enum SessionStatus { checking, unauthenticated, authenticated }

class SessionState extends Equatable {
  const SessionState._({required this.status, required this.role});

  const SessionState.checking()
    : this._(status: SessionStatus.checking, role: null);

  const SessionState.unauthenticated()
    : this._(status: SessionStatus.unauthenticated, role: null);

  const SessionState.authenticated(UserRole userRole)
    : this._(status: SessionStatus.authenticated, role: userRole);

  final SessionStatus status;
  final UserRole? role;

  bool get isChecking => status == SessionStatus.checking;

  bool get isAuthenticated => status == SessionStatus.authenticated;

  @override
  List<Object?> get props => [status, role];
}
