import 'package:equatable/equatable.dart';
import 'package:image_picker/image_picker.dart';

import '../../domain/models/merchant_restaurant.dart';

enum MerchantRestaurantStatus { initial, loading, saving, success, failure }

const Object _unset = Object();

class MerchantRestaurantState extends Equatable {
  const MerchantRestaurantState({
    required this.status,
    this.restaurants = const [],
    this.restaurant,
    this.backgroundImageFile,
    this.avatarImageFile,
    this.errorMessage,
    this.infoMessage,
  });

  const MerchantRestaurantState.initial()
    : this(status: MerchantRestaurantStatus.initial);

  final MerchantRestaurantStatus status;
  final List<MerchantRestaurant> restaurants;
  final MerchantRestaurant? restaurant;
  final XFile? backgroundImageFile;
  final XFile? avatarImageFile;
  final String? errorMessage;
  final String? infoMessage;

  bool get isLoading => status == MerchantRestaurantStatus.loading;
  bool get isSaving => status == MerchantRestaurantStatus.saving;
  bool get isBusy => isLoading || isSaving;

  MerchantRestaurantState copyWith({
    MerchantRestaurantStatus? status,
    List<MerchantRestaurant>? restaurants,
    Object? restaurant = _unset,
    Object? backgroundImageFile = _unset,
    Object? avatarImageFile = _unset,
    String? errorMessage,
    String? infoMessage,
    bool clearError = false,
    bool clearInfo = false,
  }) {
    return MerchantRestaurantState(
      status: status ?? this.status,
      restaurants: restaurants ?? this.restaurants,
      restaurant:
          restaurant == _unset
              ? this.restaurant
              : restaurant as MerchantRestaurant?,
      backgroundImageFile:
          backgroundImageFile == _unset
              ? this.backgroundImageFile
              : backgroundImageFile as XFile?,
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
    restaurants,
    restaurant,
    backgroundImageFile?.path,
    avatarImageFile?.path,
    errorMessage,
    infoMessage,
  ];
}
