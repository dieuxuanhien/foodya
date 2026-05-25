import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:geolocator/geolocator.dart';

import '../../../../core/location/geolocation_service.dart';
import '../../../../core/network/api_error_ui_message.dart';
import '../../domain/repositories/customer_catalog_repository.dart';
import 'customer_home_state.dart';

class CustomerHomeCubit extends Cubit<CustomerHomeState> {
  CustomerHomeCubit({
    required CustomerCatalogRepository catalogRepository,
    required GeolocationService geolocationService,
  }) : _catalogRepository = catalogRepository,
       _geolocationService = geolocationService,
       super(const CustomerHomeState.initial());

  final CustomerCatalogRepository _catalogRepository;
  final GeolocationService _geolocationService;

  Future<void> initialize() async {
    emit(
      state.copyWith(
        status: CustomerHomeStatus.loading,
        clearLocationMessage: true,
        clearNearbyMessage: true,
      ),
    );

    await refreshLocation();
  }

  Future<void> refreshLocation() async {
    emit(
      state.copyWith(
        isRefreshingLocation: true,
        clearLocationMessage: true,
        clearNearbyMessage: true,
      ),
    );

    try {
      final isServiceEnabled =
          await _geolocationService.isLocationServiceEnabled();
      if (!isServiceEnabled) {
        emit(
          state.copyWith(
            status: CustomerHomeStatus.success,
            isRefreshingLocation: false,
            nearbyRestaurants: const [],
            locationMessage: 'Location service is turned off.',
            nearbyMessage:
                'Turn on location service to see nearby restaurants.',
            friendlyAddress: null,
            clearCoordinates: true,
          ),
        );
        return;
      }

      final permission = await _geolocationService.requestPermission();
      if (permission == LocationPermission.denied ||
          permission == LocationPermission.deniedForever) {
        emit(
          state.copyWith(
            status: CustomerHomeStatus.success,
            isRefreshingLocation: false,
            nearbyRestaurants: const [],
            locationMessage: 'Location permission denied.',
            nearbyMessage:
                'Grant location permission to load nearby restaurants.',
            friendlyAddress: null,
            clearCoordinates: true,
          ),
        );
        return;
      }

      final position = await _geolocationService.getCurrentPosition(
        accuracy: LocationAccuracy.high,
        timeLimit: const Duration(seconds: 10),
      );

      final friendlyAddress = await _geolocationService.getFriendlyAddress(
        position,
      );

      emit(
        state.copyWith(
          latitude: position.latitude,
          longitude: position.longitude,
          friendlyAddress: friendlyAddress,
          clearLocationMessage: true,
        ),
      );

      await _loadNearby(lat: position.latitude, lng: position.longitude);
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to get your location right now.',
      );

      emit(
        state.copyWith(
          status: CustomerHomeStatus.failure,
          isRefreshingLocation: false,
          nearbyRestaurants: const [],
          locationMessage: presentation.message,
          nearbyMessage: 'Nearby restaurants are unavailable right now.',
          friendlyAddress: null,
          clearCoordinates: true,
        ),
      );
    }
  }

  Future<void> useManualLocation({
    required double latitude,
    required double longitude,
  }) async {
    final friendlyAddress = await _geolocationService
        .getFriendlyAddressForCoordinates(lat: latitude, lng: longitude);

    emit(
      state.copyWith(
        isRefreshingLocation: true,
        latitude: latitude,
        longitude: longitude,
        friendlyAddress: friendlyAddress,
        clearFriendlyAddress: friendlyAddress == null,
        clearLocationMessage: true,
        clearNearbyMessage: true,
      ),
    );
    await _loadNearby(lat: latitude, lng: longitude);
  }

  Future<void> _loadNearby({required double lat, required double lng}) async {
    emit(state.copyWith(isNearbyLoading: true, clearNearbyMessage: true));

    try {
      final page = await _catalogRepository.nearbyRestaurants(
        lat: lat,
        lng: lng,
        radiusKm: 5.0,
        sort: 'distance_asc',
        page: 0,
        size: 10,
      );

      emit(
        state.copyWith(
          status: CustomerHomeStatus.success,
          isRefreshingLocation: false,
          isNearbyLoading: false,
          nearbyRestaurants: page.items,
          nearbyMessage:
              page.items.isEmpty
                  ? 'No nearby restaurants found in your area.'
                  : null,
        ),
      );
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to load nearby restaurants.',
      );

      emit(
        state.copyWith(
          status: CustomerHomeStatus.failure,
          isRefreshingLocation: false,
          isNearbyLoading: false,
          nearbyRestaurants: const [],
          nearbyMessage: presentation.message,
        ),
      );
    }
  }
}
