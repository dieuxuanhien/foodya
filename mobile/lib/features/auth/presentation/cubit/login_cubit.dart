import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/auth/session_cubit.dart';
import '../../../../core/auth/user_role.dart';
import '../../../../core/network/api_error_ui_message.dart';
import '../../../../core/network/api_exception.dart';
import '../../domain/models/login_request.dart';
import '../../domain/models/register_request.dart';
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

  Future<void> restoreSession() async {
    if (state.status == LoginStatus.restoring) {
      return;
    }

    _sessionCubit.setChecking();
    emit(
      state.copyWith(
        status: LoginStatus.restoring,
        clearError: true,
        clearInfo: true,
        clearFieldErrors: true,
      ),
    );

    try {
      final session = await _authRepository.restoreSession();
      if (session == null) {
        _sessionCubit.signOut();
        emit(
          state.copyWith(
            status: LoginStatus.idle,
            clearError: true,
            clearInfo: true,
            clearFieldErrors: true,
          ),
        );
        return;
      }

      _sessionCubit.signInAs(session.role);
      emit(
        state.copyWith(
          status: LoginStatus.idle,
          infoMessage: 'Session restored for ${session.role.label}.',
          clearError: true,
          clearFieldErrors: true,
        ),
      );
    } catch (_) {
      await _authRepository.clearSession();
      _sessionCubit.signOut();
      emit(
        state.copyWith(
          status: LoginStatus.idle,
          clearError: true,
          clearInfo: true,
          clearFieldErrors: true,
        ),
      );
    }
  }

  void setRegistrationRole(UserRole role) {
    emit(
      state.copyWith(registrationRole: role, clearError: true, clearInfo: true),
    );
  }

  void clearFeedback() {
    emit(
      state.copyWith(clearError: true, clearInfo: true, clearFieldErrors: true),
    );
  }

  Future<void> signIn({
    required String usernameOrEmail,
    required String password,
  }) async {
    if (state.isBusy) {
      return;
    }

    if (usernameOrEmail.trim().isEmpty || password.trim().isEmpty) {
      emit(
        state.copyWith(
          status: LoginStatus.failure,
          errorMessage: 'Username/email and password are required.',
          clearInfo: true,
        ),
      );
      return;
    }

    emit(
      state.copyWith(
        status: LoginStatus.signingIn,
        clearError: true,
        clearInfo: true,
        clearFieldErrors: true,
      ),
    );

    try {
      final session = await _authRepository.login(
        LoginRequest(
          usernameOrEmail: usernameOrEmail.trim(),
          password: password,
        ),
      );
      _sessionCubit.signInAs(session.role);
      emit(
        state.copyWith(
          status: LoginStatus.idle,
          infoMessage: 'Signed in as ${session.role.label}.',
          clearError: true,
          clearFieldErrors: true,
        ),
      );
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to login.',
      );
      emit(
        state.copyWith(
          status: LoginStatus.failure,
          errorMessage: presentation.message,
          fieldErrors: presentation.fieldErrors,
          clearInfo: true,
        ),
      );
    }
  }

  Future<void> register({
    required String username,
    required String email,
    required String phoneNumber,
    required String fullName,
    required String password,
    required UserRole role,
  }) async {
    if (state.isBusy) {
      return;
    }

    if (username.trim().isEmpty ||
        email.trim().isEmpty ||
        phoneNumber.trim().isEmpty ||
        fullName.trim().isEmpty ||
        password.trim().isEmpty) {
      emit(
        state.copyWith(
          status: LoginStatus.failure,
          errorMessage: 'All register fields are required.',
          clearInfo: true,
        ),
      );
      return;
    }

    emit(
      state.copyWith(
        status: LoginStatus.registering,
        clearError: true,
        clearInfo: true,
        clearFieldErrors: true,
      ),
    );

    try {
      final session = await _authRepository.register(
        RegisterRequest(
          username: username.trim(),
          email: email.trim(),
          phoneNumber: phoneNumber.trim(),
          fullName: fullName.trim(),
          password: password,
          role: role,
        ),
      );
      _sessionCubit.signInAs(session.role);
      emit(
        state.copyWith(
          status: LoginStatus.idle,
          infoMessage:
              'Account created and signed in as ${session.role.label}.',
          clearError: true,
          clearFieldErrors: true,
        ),
      );
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to register account.',
      );
      emit(
        state.copyWith(
          status: LoginStatus.failure,
          errorMessage: presentation.message,
          fieldErrors: presentation.fieldErrors,
          clearInfo: true,
        ),
      );
    }
  }

  Future<void> refreshToken() async {
    if (state.isBusy) {
      return;
    }

    emit(
      state.copyWith(
        status: LoginStatus.refreshing,
        clearError: true,
        clearInfo: true,
        clearFieldErrors: true,
      ),
    );

    try {
      final session = await _authRepository.refreshSession();
      _sessionCubit.signInAs(session.role);
      emit(
        state.copyWith(
          status: LoginStatus.idle,
          infoMessage: 'Session token refreshed.',
          clearError: true,
          clearFieldErrors: true,
        ),
      );
    } catch (error) {
      if (error is ApiException && error.statusCode == 401) {
        await _authRepository.clearSession();
        _sessionCubit.signOut();
      }
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to refresh token.',
      );
      emit(
        state.copyWith(
          status: LoginStatus.failure,
          errorMessage: presentation.message,
          fieldErrors: presentation.fieldErrors,
          clearInfo: true,
        ),
      );
    }
  }

  Future<void> logoutAll() async {
    if (state.isBusy) {
      return;
    }

    emit(
      state.copyWith(
        status: LoginStatus.loggingOutAll,
        clearError: true,
        clearInfo: true,
        clearFieldErrors: true,
      ),
    );

    try {
      await _authRepository.logoutAll();
      _sessionCubit.signOut();
      emit(
        state.copyWith(
          status: LoginStatus.idle,
          infoMessage: 'Logged out from all sessions.',
          clearError: true,
          clearFieldErrors: true,
        ),
      );
    } catch (error) {
      if (error is ApiException && error.statusCode == 401) {
        await _authRepository.clearSession();
        _sessionCubit.signOut();
      }
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to logout all sessions.',
      );
      emit(
        state.copyWith(
          status: LoginStatus.failure,
          errorMessage: presentation.message,
          fieldErrors: presentation.fieldErrors,
          clearInfo: true,
        ),
      );
    }
  }
}
