import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/network/api_error_ui_message.dart';
import '../../domain/repositories/customer_profile_repository.dart';
import 'profile_state.dart';

class ProfileCubit extends Cubit<ProfileState> {
  ProfileCubit({required CustomerProfileRepository repository})
    : _repository = repository,
      super(const ProfileState.initial());

  final CustomerProfileRepository _repository;

  Future<void> load() async {
    emit(
      state.copyWith(
        status: ProfileStatus.loading,
        clearError: true,
        clearInfo: true,
      ),
    );
    try {
      final profile = await _repository.me();
      emit(
        state.copyWith(
          status: ProfileStatus.success,
          profile: profile,
          clearError: true,
        ),
      );
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to load profile.',
      );
      emit(
        state.copyWith(
          status: ProfileStatus.failure,
          errorMessage: presentation.message,
        ),
      );
    }
  }

  Future<void> save({
    required String fullName,
    required String email,
    required String phoneNumber,
    String? avatarUrl,
  }) async {
    if (state.isBusy) {
      return;
    }
    emit(
      state.copyWith(
        status: ProfileStatus.saving,
        clearError: true,
        clearInfo: true,
      ),
    );
    try {
      final profile = await _repository.updateProfile(
        fullName: fullName.trim(),
        email: email.trim(),
        phoneNumber: phoneNumber.trim(),
        avatarUrl: avatarUrl?.trim(),
      );
      emit(
        state.copyWith(
          status: ProfileStatus.success,
          profile: profile,
          infoMessage: 'Profile updated.',
          clearError: true,
        ),
      );
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to update profile.',
      );
      emit(
        state.copyWith(
          status: ProfileStatus.failure,
          errorMessage: presentation.message,
        ),
      );
    }
  }

  Future<void> changePassword({
    required String currentPassword,
    required String newPassword,
    required String confirmPassword,
  }) async {
    if (state.isBusy) {
      return;
    }
    emit(
      state.copyWith(
        status: ProfileStatus.changingPassword,
        clearError: true,
        clearInfo: true,
      ),
    );
    try {
      await _repository.changePassword(
        currentPassword: currentPassword,
        newPassword: newPassword,
        confirmPassword: confirmPassword,
      );
      emit(
        state.copyWith(
          status: ProfileStatus.success,
          infoMessage: 'Password updated.',
          clearError: true,
        ),
      );
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to update password.',
      );
      emit(
        state.copyWith(
          status: ProfileStatus.failure,
          errorMessage: presentation.message,
        ),
      );
    }
  }

  void clearFeedback() {
    emit(state.copyWith(clearError: true, clearInfo: true));
  }
}
