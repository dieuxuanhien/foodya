import 'package:equatable/equatable.dart';
import 'package:image_picker/image_picker.dart';

import '../../domain/models/user_profile.dart';

enum ProfileStatus {
  initial,
  loading,
  success,
  saving,
  changingPassword,
  failure,
}

const Object _unset = Object();

class ProfileState extends Equatable {
  const ProfileState({
    required this.status,
    this.profile,
    this.avatarImageFile,
    this.errorMessage,
    this.infoMessage,
  });

  const ProfileState.initial() : this(status: ProfileStatus.initial);

  final ProfileStatus status;
  final UserProfile? profile;
  final XFile? avatarImageFile;
  final String? errorMessage;
  final String? infoMessage;

  bool get isBusy =>
      status == ProfileStatus.loading ||
      status == ProfileStatus.saving ||
      status == ProfileStatus.changingPassword;

  ProfileState copyWith({
    ProfileStatus? status,
    UserProfile? profile,
    Object? avatarImageFile = _unset,
    String? errorMessage,
    String? infoMessage,
    bool clearError = false,
    bool clearInfo = false,
  }) {
    return ProfileState(
      status: status ?? this.status,
      profile: profile ?? this.profile,
      avatarImageFile:
          avatarImageFile == _unset
              ? this.avatarImageFile
              : avatarImageFile as XFile?,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      infoMessage: clearInfo ? null : (infoMessage ?? this.infoMessage),
    );
  }

  @override
  List<Object?> get props => [
    status,
    profile,
    avatarImageFile?.path,
    errorMessage,
    infoMessage,
  ];
}
