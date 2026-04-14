import 'package:equatable/equatable.dart';

enum LoginStatus { idle, submitting, success, failure }

class LoginState extends Equatable {
  const LoginState({required this.status, this.errorMessage});

  const LoginState.initial() : this(status: LoginStatus.idle);

  final LoginStatus status;
  final String? errorMessage;

  bool get isSubmitting => status == LoginStatus.submitting;

  LoginState copyWith({LoginStatus? status, String? errorMessage}) {
    return LoginState(
      status: status ?? this.status,
      errorMessage: errorMessage,
    );
  }

  @override
  List<Object?> get props => [status, errorMessage];
}
