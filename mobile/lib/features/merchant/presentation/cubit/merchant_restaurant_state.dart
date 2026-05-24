import 'package:equatable/equatable.dart';

import '../../domain/models/merchant_restaurant.dart';

enum MerchantRestaurantStatus { initial, saving, success, failure }

class MerchantRestaurantState extends Equatable {
  const MerchantRestaurantState({
    required this.status,
    this.restaurant,
    this.errorMessage,
    this.infoMessage,
  });

  const MerchantRestaurantState.initial()
    : this(status: MerchantRestaurantStatus.initial);

  final MerchantRestaurantStatus status;
  final MerchantRestaurant? restaurant;
  final String? errorMessage;
  final String? infoMessage;

  bool get isSaving => status == MerchantRestaurantStatus.saving;

  MerchantRestaurantState copyWith({
    MerchantRestaurantStatus? status,
    MerchantRestaurant? restaurant,
    String? errorMessage,
    String? infoMessage,
    bool clearError = false,
    bool clearInfo = false,
  }) {
    return MerchantRestaurantState(
      status: status ?? this.status,
      restaurant: restaurant ?? this.restaurant,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      infoMessage: clearInfo ? null : (infoMessage ?? this.infoMessage),
    );
  }

  @override
  List<Object?> get props => [status, restaurant, errorMessage, infoMessage];
}
