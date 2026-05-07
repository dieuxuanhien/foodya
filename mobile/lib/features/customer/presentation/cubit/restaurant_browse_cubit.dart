import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/network/api_error_ui_message.dart';
import '../../domain/repositories/customer_catalog_repository.dart';
import 'restaurant_browse_state.dart';

class RestaurantBrowseCubit extends Cubit<RestaurantBrowseState> {
  RestaurantBrowseCubit({required CustomerCatalogRepository repository})
    : _repository = repository,
      super(const RestaurantBrowseState.initial());

  final CustomerCatalogRepository _repository;

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
    await _fetchPage(reset: true);
  }

  Future<void> loadMore() async {
    if (!state.hasMore || state.status == RestaurantBrowseStatus.loadingMore) {
      return;
    }

    await _fetchPage(reset: false);
  }

  Future<void> _fetchPage({required bool reset}) async {
    final nextPage = reset ? 0 : state.page + 1;

    emit(
      state.copyWith(
        status:
            reset
                ? RestaurantBrowseStatus.loading
                : RestaurantBrowseStatus.loadingMore,
        clearError: true,
      ),
    );

    try {
      final page = await _repository.searchRestaurants(
        keyword: state.keyword.trim().isEmpty ? null : state.keyword.trim(),
        minRating: state.minRating,
        openNow: state.openNow,
        sort: state.sort,
        taxonomyCodes: state.selectedTaxonomyCodes,
        page: nextPage,
        size: 10,
      );

      final merged = reset ? page.items : [...state.restaurants, ...page.items];

      emit(
        state.copyWith(
          status:
              merged.isEmpty
                  ? RestaurantBrowseStatus.empty
                  : RestaurantBrowseStatus.success,
          restaurants: merged,
          page: page.page,
          hasMore: page.hasNextPage,
          clearError: true,
        ),
      );
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to load restaurants.',
      );

      if (!reset && state.restaurants.isNotEmpty) {
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
