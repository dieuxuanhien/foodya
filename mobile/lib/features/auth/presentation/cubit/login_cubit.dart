import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/auth/session_cubit.dart';
import '../../../../core/auth/user_role.dart';
import '../../domain/repositories/auth_repository.dart';
import 'login_state.dart';

class LoginCubit extends Cubit<LoginState> {
  LoginCubit({
    required AuthRepository authRepository,
    required SessionCubit sessionCubit,
  }) : _authRepository = authRepository,
       _sessionCubit = sessionCubit,
       super(const LoginState.initial());

  final AuthRepository _authRepository;
  final SessionCubit _sessionCubit;

  Future<void> loginAs(UserRole role) async {
    if (state.isSubmitting) {
      return;
    }

    emit(state.copyWith(status: LoginStatus.submitting, errorMessage: null));

    try {
      await _authRepository.loginAs(role);
      _sessionCubit.signInAs(role);
      emit(state.copyWith(status: LoginStatus.success, errorMessage: null));
    } catch (_) {
      emit(
        state.copyWith(
          status: LoginStatus.failure,
          errorMessage: 'Unable to login. Please try again.',
        ),
      );
    }
  }

  Future<void> logout() async {
    await _authRepository.logout();
    _sessionCubit.signOut();
    emit(const LoginState.initial());
  }
}
