import 'package:equatable/equatable.dart';

import '../../../../core/auth/user_role.dart';

enum LoginStatus {
  idle,
  restoring,
  signingIn,
  registering,
  refreshing,
  loggingOutAll,
  failure,
}

class LoginState extends Equatable {
  const LoginState({
    required this.status,
    required this.registrationRole,
    this.errorMessage,
    this.infoMessage,
    this.fieldErrors = const {},
  });

  const LoginState.initial()
    : this(status: LoginStatus.idle, registrationRole: UserRole.customer);

  final LoginStatus status;
  final UserRole registrationRole;
  final String? errorMessage;
  final String? infoMessage;
  final Map<String, String> fieldErrors;

  bool get isBusy {
    return status == LoginStatus.restoring ||
        status == LoginStatus.signingIn ||
        status == LoginStatus.registering ||
        status == LoginStatus.refreshing ||
        status == LoginStatus.loggingOutAll;
  }

  LoginState copyWith({
    LoginStatus? status,
    UserRole? registrationRole,
    String? errorMessage,
    String? infoMessage,
    Map<String, String>? fieldErrors,
    bool clearError = false,
    bool clearInfo = false,
    bool clearFieldErrors = false,
  }) {
    final resolvedFieldErrors =
        fieldErrors ?? (clearFieldErrors ? const {} : this.fieldErrors);

    return LoginState(
      status: status ?? this.status,
      registrationRole: registrationRole ?? this.registrationRole,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      infoMessage: clearInfo ? null : (infoMessage ?? this.infoMessage),
      fieldErrors: resolvedFieldErrors,
    );
  }

  @override
  List<Object?> get props => [
    status,
    registrationRole,
    errorMessage,
    infoMessage,
    fieldErrors,
  ];
}
