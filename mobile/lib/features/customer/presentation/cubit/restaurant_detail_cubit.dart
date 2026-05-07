import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/network/api_error_ui_message.dart';
import '../../domain/repositories/customer_catalog_repository.dart';
import 'restaurant_detail_state.dart';

class RestaurantDetailCubit extends Cubit<RestaurantDetailState> {
  RestaurantDetailCubit({required CustomerCatalogRepository repository})
    : _repository = repository,
      super(const RestaurantDetailState.initial());

  final CustomerCatalogRepository _repository;

  String? _restaurantId;

  Future<void> load(String restaurantId) async {
    _restaurantId = restaurantId;
    emit(
      state.copyWith(status: RestaurantDetailStatus.loading, clearError: true),
    );

    try {
      final taxonomies = await _repository.listCategoryTaxonomies();
      final detail = await _repository.getRestaurantDetail(restaurantId);
      emit(
        state.copyWith(
          taxonomies: taxonomies,
          restaurant: detail,
          clearError: true,
        ),
      );
      await refreshMenu();
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to load restaurant details.',
      );
      emit(
        state.copyWith(
          status: RestaurantDetailStatus.failure,
          errorMessage: presentation.message,
        ),
      );
    }
  }

  Future<void> updateMenuKeyword(String keyword) async {
    emit(state.copyWith(menuKeyword: keyword));
    await refreshMenu();
  }

  Future<void> updateMenuSort(String sort) async {
    emit(state.copyWith(menuSort: sort));
    await refreshMenu();
  }

  Future<void> toggleTaxonomy(String code) async {
    final selected = [...state.selectedTaxonomyCodes];
    if (selected.contains(code)) {
      selected.remove(code);
    } else {
      selected.add(code);
    }
    emit(state.copyWith(selectedTaxonomyCodes: selected));
    await refreshMenu();
  }

  Future<void> refreshMenu() async {
    await _fetchMenu(reset: true);
  }

  Future<void> loadMoreMenu() async {
    if (!state.hasMore || state.status == RestaurantDetailStatus.loadingMore) {
      return;
    }

    await _fetchMenu(reset: false);
  }

  Future<void> _fetchMenu({required bool reset}) async {
    final restaurantId = _restaurantId;
    if (restaurantId == null) {
      return;
    }

    final nextPage = reset ? 0 : state.page + 1;

    emit(
      state.copyWith(
        status:
            reset
                ? RestaurantDetailStatus.loading
                : RestaurantDetailStatus.loadingMore,
        clearError: true,
      ),
    );

    try {
      final page = await _repository.getRestaurantMenuItems(
        restaurantId,
        keyword: state.menuKeyword.trim().isEmpty ? null : state.menuKeyword,
        sort: state.menuSort,
        taxonomyCodes: state.selectedTaxonomyCodes,
        page: nextPage,
        size: 20,
      );

      final merged = reset ? page.items : [...state.menuItems, ...page.items];

      emit(
        state.copyWith(
          status:
              merged.isEmpty
                  ? RestaurantDetailStatus.empty
                  : RestaurantDetailStatus.success,
          menuItems: merged,
          page: page.page,
          hasMore: page.hasNextPage,
          clearError: true,
        ),
      );
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to load menu items.',
      );

      if (state.restaurant != null) {
        emit(
          state.copyWith(
            status:
                state.menuItems.isEmpty
                    ? RestaurantDetailStatus.empty
                    : RestaurantDetailStatus.success,
            errorMessage: presentation.message,
          ),
        );
        return;
      }

      emit(
        state.copyWith(
          status: RestaurantDetailStatus.failure,
          errorMessage: presentation.message,
        ),
      );
    }
  }
}
