import 'package:equatable/equatable.dart';

import '../../domain/models/merchant_restaurant.dart';

enum MerchantRestaurantStatus { initial, loading, saving, success, failure }

class MerchantRestaurantState extends Equatable {
  const MerchantRestaurantState({
    required this.status,
    this.restaurants = const [],
    this.restaurant,
    this.errorMessage,
    this.infoMessage,
  });

  const MerchantRestaurantState.initial()
    : this(status: MerchantRestaurantStatus.initial);

  final MerchantRestaurantStatus status;
  final List<MerchantRestaurant> restaurants;
  final MerchantRestaurant? restaurant;
  final String? errorMessage;
  final String? infoMessage;

  bool get isLoading => status == MerchantRestaurantStatus.loading;
  bool get isSaving => status == MerchantRestaurantStatus.saving;
  bool get isBusy => isLoading || isSaving;

  MerchantRestaurantState copyWith({
    MerchantRestaurantStatus? status,
    List<MerchantRestaurant>? restaurants,
    MerchantRestaurant? restaurant,
    String? errorMessage,
    String? infoMessage,
    bool clearError = false,
    bool clearInfo = false,
  }) {
    return MerchantRestaurantState(
      status: status ?? this.status,
      restaurants: restaurants ?? this.restaurants,
      restaurant: restaurant ?? this.restaurant,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      infoMessage: clearInfo ? null : (infoMessage ?? this.infoMessage),
    );
  }

  @override
  List<Object?> get props => [
    status,
    restaurants,
    restaurant,
    errorMessage,
    infoMessage,
  ];
}
