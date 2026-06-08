import 'package:equatable/equatable.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../domain/models/merchant_restaurant.dart';

class MerchantRestaurantSelectionState extends Equatable {
  const MerchantRestaurantSelectionState({this.restaurantId});

  final String? restaurantId;

  @override
  List<Object?> get props => [restaurantId];
}

class MerchantRestaurantSelectionCubit
    extends Cubit<MerchantRestaurantSelectionState> {
  MerchantRestaurantSelectionCubit()
    : super(const MerchantRestaurantSelectionState());

  void selectRestaurant(MerchantRestaurant restaurant) {
    selectRestaurantId(restaurant.id);
  }

  void selectRestaurantId(String? restaurantId) {
    if (state.restaurantId == restaurantId) {
      return;
    }
    emit(MerchantRestaurantSelectionState(restaurantId: restaurantId));
  }
}
