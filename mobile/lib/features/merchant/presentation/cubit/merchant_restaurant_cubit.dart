import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/network/api_error_ui_message.dart';
import '../../domain/models/merchant_restaurant_request.dart';
import '../../domain/repositories/merchant_restaurant_repository.dart';
import 'merchant_restaurant_state.dart';

class MerchantRestaurantCubit extends Cubit<MerchantRestaurantState> {
  MerchantRestaurantCubit({required MerchantRestaurantRepository repository})
    : _repository = repository,
      super(const MerchantRestaurantState.initial());

  final MerchantRestaurantRepository _repository;

  Future<void> create(MerchantRestaurantRequest request) async {
    if (state.isSaving) {
      return;
    }
    emit(
      state.copyWith(
        status: MerchantRestaurantStatus.saving,
        clearError: true,
        clearInfo: true,
      ),
    );
    try {
      final restaurant = await _repository.createRestaurant(request);
      emit(
        state.copyWith(
          status: MerchantRestaurantStatus.success,
          restaurant: restaurant,
          infoMessage: 'Restaurant created.',
          clearError: true,
        ),
      );
    } catch (error) {
      _emitFailure(error, 'Unable to create restaurant.');
    }
  }

  Future<void> update({
    required String restaurantId,
    required MerchantRestaurantRequest request,
  }) async {
    if (state.isSaving) {
      return;
    }
    emit(
      state.copyWith(
        status: MerchantRestaurantStatus.saving,
        clearError: true,
        clearInfo: true,
      ),
    );
    try {
      final restaurant = await _repository.updateRestaurant(
        restaurantId: restaurantId,
        request: request,
      );
      emit(
        state.copyWith(
          status: MerchantRestaurantStatus.success,
          restaurant: restaurant,
          infoMessage: 'Restaurant updated.',
          clearError: true,
        ),
      );
    } catch (error) {
      _emitFailure(error, 'Unable to update restaurant.');
    }
  }

  Future<void> setOpen({
    required String restaurantId,
    required MerchantRestaurantRequest request,
    required bool isOpen,
  }) {
    return update(
      restaurantId: restaurantId,
      request: MerchantRestaurantRequest(
        name: request.name,
        cuisineType: request.cuisineType,
        cuisineTypes: request.cuisineTypes,
        description: request.description,
        addressLine: request.addressLine,
        latitude: request.latitude,
        longitude: request.longitude,
        maxDeliveryKm: request.maxDeliveryKm,
        isOpen: isOpen,
      ),
    );
  }

  void clearFeedback() {
    emit(state.copyWith(clearError: true, clearInfo: true));
  }

  void _emitFailure(Object error, String fallback) {
    final presentation = ApiErrorUiMessageMapper.mapAny(
      error,
      fallback: fallback,
    );
    emit(
      state.copyWith(
        status: MerchantRestaurantStatus.failure,
        errorMessage: presentation.message,
      ),
    );
  }
}
