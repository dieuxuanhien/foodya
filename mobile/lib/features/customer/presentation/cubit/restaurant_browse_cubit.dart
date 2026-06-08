import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/network/api_error_ui_message.dart';
import 'package:geolocator/geolocator.dart';
import '../../../../core/location/geolocation_service.dart';
import '../../domain/repositories/customer_catalog_repository.dart';
import 'restaurant_browse_state.dart';

class RestaurantBrowseCubit extends Cubit<RestaurantBrowseState> {
  static const int restaurantPageSize = 10;

  RestaurantBrowseCubit({
    required CustomerCatalogRepository repository,
    GeolocationService? geolocationService,
  }) : _repository = repository,
       _geolocationService = geolocationService,
       super(const RestaurantBrowseState.initial());

  final CustomerCatalogRepository _repository;
  final GeolocationService? _geolocationService;

  Future<void> initialize() async {
    emit(
      state.copyWith(status: RestaurantBrowseStatus.loading, clearError: true),
    );

    try {
      final taxonomies = await _repository.listCategoryTaxonomies();
      emit(state.copyWith(taxonomies: taxonomies, clearError: true));
      await refresh();
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to load restaurant catalog.',
      );
      emit(
        state.copyWith(
          status: RestaurantBrowseStatus.failure,
          errorMessage: presentation.message,
        ),
      );
    }
  }

  void updateKeyword(String keyword) {
    emit(state.copyWith(keyword: keyword));
  }

  Future<void> applyKeyword(String keyword) async {
    updateKeyword(keyword);
    await refresh();
  }

  Future<void> changeSort(String sort) async {
    emit(state.copyWith(sort: sort));
    await refresh();
  }

  Future<void> toggleNearby(bool enabled) async {
    if (!enabled) {
      emit(
        state.copyWith(
          isNearby: false,
          clearCoordinates: true,
          sort: state.sort == 'distance_asc' ? 'relevance' : state.sort,
        ),
      );
      await refresh();
      return;
    }

    if (_geolocationService == null) {
      emit(state.copyWith(errorMessage: 'Location services not available.'));
      return;
    }

    try {
      final permission = await _geolocationService.requestPermission();
      if (permission == LocationPermission.denied ||
          permission == LocationPermission.deniedForever) {
        emit(state.copyWith(errorMessage: 'Location permission denied.'));
        return;
      }

      final pos = await _geolocationService.getCurrentPosition();
      emit(
        state.copyWith(
          isNearby: true,
          latitude: pos.latitude,
          longitude: pos.longitude,
        ),
      );

      await refresh();
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to access location. Please try again.',
      );
      emit(state.copyWith(errorMessage: presentation.message));
    }
  }

  Future<void> useManualNearbyLocation({
    required double latitude,
    required double longitude,
  }) async {
    emit(
      state.copyWith(
        isNearby: true,
        latitude: latitude,
        longitude: longitude,
        sort: state.sort == 'relevance' ? 'distance_asc' : state.sort,
        clearError: true,
      ),
    );
    await refresh();
  }

  Future<void> toggleOpenNow(bool enabled) async {
    emit(state.copyWith(openNow: enabled ? true : null));
    await refresh();
  }

  Future<void> setMinRating(double? minRating) async {
    emit(state.copyWith(minRating: minRating));
    await refresh();
  }

  Future<void> toggleTaxonomy(String code) async {
    final selected = [...state.selectedTaxonomyCodes];
    if (selected.contains(code)) {
      selected.remove(code);
    } else {
      selected.add(code);
    }

    emit(state.copyWith(selectedTaxonomyCodes: selected));
    await refresh();
  }

  Future<void> clearFilters() async {
    emit(
      state.copyWith(
        selectedTaxonomyCodes: const [],
        resetOpenNow: true,
        resetMinRating: true,
        sort: 'relevance',
      ),
    );
    await refresh();
  }

  Future<void> refresh() async {
    await _fetchPage(pageIndex: 0);
  }

  Future<void> nextPage() async {
    if (!state.hasMore || state.isBusy) {
      return;
    }

    await goToPage(state.page + 1);
  }

  Future<void> previousPage() async {
    if (state.page == 0 || state.isBusy) {
      return;
    }

    await goToPage(state.page - 1);
  }

  Future<void> goToPage(int pageIndex) async {
    if (pageIndex < 0 || state.isBusy) {
      return;
    }

    if (state.totalPages > 0 && pageIndex >= state.totalPages) {
      return;
    }

    await _fetchPage(pageIndex: pageIndex);
  }

  Future<void> _fetchPage({required int pageIndex}) async {
    emit(
      state.copyWith(status: RestaurantBrowseStatus.loading, clearError: true),
    );

    try {
      final result =
          state.isNearby && state.latitude != null && state.longitude != null
              ? await _repository.nearbyRestaurants(
                lat: state.latitude!,
                lng: state.longitude!,
                radiusKm: state.radiusKm,
                sort: state.sort,
                page: pageIndex,
                size: restaurantPageSize,
              )
              : await _repository.searchRestaurants(
                keyword:
                    state.keyword.trim().isEmpty ? null : state.keyword.trim(),
                minRating: state.minRating,
                openNow: state.openNow,
                sort: state.sort,
                taxonomyCodes: state.selectedTaxonomyCodes,
                page: pageIndex,
                size: restaurantPageSize,
              );

      emit(
        state.copyWith(
          status:
              result.items.isEmpty
                  ? RestaurantBrowseStatus.empty
                  : RestaurantBrowseStatus.success,
          restaurants: result.items,
          page: result.page,
          totalPages: result.totalPages,
          totalElements: result.totalElements,
          hasMore: result.hasNextPage,
          clearError: true,
        ),
      );
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to load restaurants.',
      );

      if (state.restaurants.isNotEmpty) {
        emit(
          state.copyWith(
            status: RestaurantBrowseStatus.success,
            errorMessage: presentation.message,
          ),
        );
        return;
      }

      emit(
        state.copyWith(
          status: RestaurantBrowseStatus.failure,
          errorMessage: presentation.message,
        ),
      );
    }
  }
}
