import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:image_picker/image_picker.dart';

import '../../../../core/network/api_error_ui_message.dart';
import '../../domain/models/merchant_restaurant.dart';
import '../../domain/models/merchant_restaurant_request.dart';
import '../../domain/repositories/merchant_restaurant_repository.dart';
import 'merchant_restaurant_selection_cubit.dart';
import 'merchant_restaurant_state.dart';

class MerchantRestaurantCubit extends Cubit<MerchantRestaurantState> {
  MerchantRestaurantCubit({
    required MerchantRestaurantRepository repository,
    MerchantRestaurantSelectionCubit? selectionCubit,
  }) : _repository = repository,
       _selectionCubit = selectionCubit,
       super(const MerchantRestaurantState.initial());

  final MerchantRestaurantRepository _repository;
  final MerchantRestaurantSelectionCubit? _selectionCubit;

  Future<void> loadRestaurants() async {
    if (state.isBusy) {
      return;
    }
    emit(
      state.copyWith(
        status: MerchantRestaurantStatus.loading,
        clearError: true,
        clearInfo: true,
      ),
    );
    try {
      final restaurants = await _repository.listRestaurants();
      final selected = _resolveSelectedRestaurant(restaurants);
      emit(
        state.copyWith(
          status: MerchantRestaurantStatus.success,
          restaurants: restaurants,
          restaurant: selected,
          clearError: true,
        ),
      );
    } catch (error) {
      _emitFailure(error, 'Unable to load restaurants.');
    }
  }

  void selectRestaurant(MerchantRestaurant restaurant) {
    _selectionCubit?.selectRestaurant(restaurant);
    emit(state.copyWith(restaurant: restaurant, clearError: true));
  }

  void setBackgroundImageFile(XFile? file) {
    emit(state.copyWith(backgroundImageFile: file));
  }

  void setAvatarImageFile(XFile? file) {
    emit(state.copyWith(avatarImageFile: file));
  }

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
      final restaurant = await _repository.createRestaurant(
        request,
        backgroundFile: state.backgroundImageFile,
        avatarFile: state.avatarImageFile,
      );
      emit(
        state.copyWith(
          status: MerchantRestaurantStatus.success,
          restaurants: _upsertRestaurant(state.restaurants, restaurant),
          restaurant: restaurant,
          backgroundImageFile: null,
          avatarImageFile: null,
          infoMessage: 'Restaurant created.',
          clearError: true,
        ),
      );
      _selectionCubit?.selectRestaurant(restaurant);
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
        backgroundFile: state.backgroundImageFile,
        avatarFile: state.avatarImageFile,
      );
      emit(
        state.copyWith(
          status: MerchantRestaurantStatus.success,
          restaurants: _upsertRestaurant(state.restaurants, restaurant),
          restaurant: restaurant,
          backgroundImageFile: null,
          avatarImageFile: null,
          infoMessage: 'Restaurant updated.',
          clearError: true,
        ),
      );
      _selectionCubit?.selectRestaurant(restaurant);
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

  List<MerchantRestaurant> _upsertRestaurant(
    List<MerchantRestaurant> restaurants,
    MerchantRestaurant restaurant,
  ) {
    final index = restaurants.indexWhere((item) => item.id == restaurant.id);
    if (index == -1) {
      return [restaurant, ...restaurants];
    }
    return [
      for (var i = 0; i < restaurants.length; i++)
        if (i == index) restaurant else restaurants[i],
    ];
  }

  MerchantRestaurant? _resolveSelectedRestaurant(
    List<MerchantRestaurant> restaurants,
  ) {
    if (restaurants.isEmpty) {
      _selectionCubit?.selectRestaurantId(null);
      return null;
    }
    final selectedId = _selectionCubit?.state.restaurantId;
    if (selectedId != null) {
      final selected = _findRestaurant(restaurants, selectedId);
      if (selected != null) {
        return selected;
      }
    }
    final current = state.restaurant;
    if (current != null) {
      final selected = _findRestaurant(restaurants, current.id);
      if (selected != null) {
        return selected;
      }
    }
    final selected = restaurants.first;
    _selectionCubit?.selectRestaurant(selected);
    return selected;
  }

  MerchantRestaurant? _findRestaurant(
    List<MerchantRestaurant> restaurants,
    String restaurantId,
  ) {
    for (final restaurant in restaurants) {
      if (restaurant.id == restaurantId) {
        return restaurant;
      }
    }
    return null;
  }
}
