import 'package:equatable/equatable.dart';

import '../../domain/models/user_profile.dart';

enum ProfileStatus {
  initial,
  loading,
  success,
  saving,
  changingPassword,
  failure,
}

class ProfileState extends Equatable {
  const ProfileState({
    required this.status,
    this.profile,
    this.errorMessage,
    this.infoMessage,
  });

  const ProfileState.initial() : this(status: ProfileStatus.initial);

  final ProfileStatus status;
  final UserProfile? profile;
  final String? errorMessage;
  final String? infoMessage;

  bool get isBusy =>
      status == ProfileStatus.loading ||
      status == ProfileStatus.saving ||
      status == ProfileStatus.changingPassword;

  ProfileState copyWith({
    ProfileStatus? status,
    UserProfile? profile,
    String? errorMessage,
    String? infoMessage,
    bool clearError = false,
    bool clearInfo = false,
  }) {
    return ProfileState(
      status: status ?? this.status,
      profile: profile ?? this.profile,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      infoMessage: clearInfo ? null : (infoMessage ?? this.infoMessage),
    );
  }

  @override
  List<Object?> get props => [status, profile, errorMessage, infoMessage];
}
