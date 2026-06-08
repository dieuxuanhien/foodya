import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:image_picker/image_picker.dart';

import '../../../../core/network/api_error_ui_message.dart';
import '../../domain/models/merchant_menu_category.dart';
import '../../domain/models/merchant_menu_category_request.dart';
import '../../domain/models/merchant_menu_item.dart';
import '../../domain/models/merchant_menu_item_request.dart';
import '../../domain/models/merchant_restaurant.dart';
import '../../domain/repositories/merchant_catalog_repository.dart';
import '../../domain/repositories/merchant_restaurant_repository.dart';
import 'merchant_catalog_state.dart';

class MerchantCatalogCubit extends Cubit<MerchantCatalogState> {
  MerchantCatalogCubit({
    required MerchantCatalogRepository catalogRepository,
    required MerchantRestaurantRepository restaurantRepository,
  }) : _catalogRepository = catalogRepository,
       _restaurantRepository = restaurantRepository,
       super(const MerchantCatalogState.initial());

  final MerchantCatalogRepository _catalogRepository;
  final MerchantRestaurantRepository _restaurantRepository;

  Future<void> load() async {
    if (state.isBusy) {
      return;
    }
    emit(
      state.copyWith(
        status: MerchantCatalogStatus.loading,
        clearError: true,
        clearInfo: true,
      ),
    );
    try {
      final restaurants = await _restaurantRepository.listRestaurants();
      final taxonomies = await _catalogRepository.listCategoryTaxonomies();
      final selected =
          state.selectedRestaurant ??
          (restaurants.isEmpty ? null : restaurants.first);
      emit(
        state.copyWith(
          status: MerchantCatalogStatus.success,
          restaurants: restaurants,
          taxonomies: taxonomies,
          selectedRestaurant: selected,
          clearError: true,
        ),
      );
      if (selected != null) {
        await loadRestaurantCatalog(selected, preserveSelection: false);
      }
    } catch (error) {
      _emitFailure(error, 'Unable to load merchant catalog.');
    }
  }

  Future<void> loadRestaurantCatalog(
    MerchantRestaurant restaurant, {
    bool preserveSelection = true,
  }) async {
    if (state.isSaving) {
      return;
    }
    emit(
      state.copyWith(
        status: MerchantCatalogStatus.loading,
        selectedRestaurant: restaurant,
        selectedCategory: preserveSelection ? state.selectedCategory : null,
        selectedItem: preserveSelection ? state.selectedItem : null,
        clearError: true,
        clearInfo: true,
      ),
    );
    try {
      final categories = await _catalogRepository.listCategories(restaurant.id);
      final items = await _catalogRepository.listMenuItems(restaurant.id);
      emit(
        state.copyWith(
          status: MerchantCatalogStatus.success,
          categories: categories,
          items: items,
          selectedCategory: preserveSelection ? state.selectedCategory : null,
          selectedItem: preserveSelection ? state.selectedItem : null,
          clearError: true,
        ),
      );
    } catch (error) {
      _emitFailure(error, 'Unable to load menu catalog.');
    }
  }

  void selectCategory(MerchantMenuCategory category) {
    emit(state.copyWith(selectedCategory: category, clearError: true));
  }

  void clearCategorySelection() {
    emit(state.copyWith(selectedCategory: null, clearError: true));
  }

  void selectItem(MerchantMenuItem item) {
    emit(state.copyWith(selectedItem: item, clearError: true));
  }

  void clearItemSelection() {
    emit(state.copyWith(selectedItem: null, clearError: true));
  }

  void setMenuItemImageFile(XFile? file) {
    emit(state.copyWith(menuItemImageFile: file));
  }

  Future<void> createCategory(MerchantMenuCategoryRequest request) async {
    final restaurant = state.selectedRestaurant;
    if (restaurant == null || state.isSaving) {
      return;
    }
    emit(_saving());
    try {
      final category = await _catalogRepository.createCategory(
        restaurantId: restaurant.id,
        request: request,
      );
      emit(
        state.copyWith(
          status: MerchantCatalogStatus.success,
          categories: _upsertCategory(state.categories, category),
          selectedCategory: category,
          infoMessage: 'Category created.',
          clearError: true,
        ),
      );
    } catch (error) {
      _emitFailure(error, 'Unable to create category.');
    }
  }

  Future<void> updateCategory({
    required String categoryId,
    required MerchantMenuCategoryRequest request,
  }) async {
    if (state.isSaving) {
      return;
    }
    emit(_saving());
    try {
      final category = await _catalogRepository.updateCategory(
        categoryId: categoryId,
        request: request,
      );
      emit(
        state.copyWith(
          status: MerchantCatalogStatus.success,
          categories: _upsertCategory(state.categories, category),
          selectedCategory: category,
          infoMessage: 'Category updated.',
          clearError: true,
        ),
      );
    } catch (error) {
      _emitFailure(error, 'Unable to update category.');
    }
  }

  Future<void> deleteCategory(String categoryId) async {
    if (state.isSaving) {
      return;
    }
    emit(_saving());
    try {
      await _catalogRepository.deleteCategory(categoryId);
      emit(
        state.copyWith(
          status: MerchantCatalogStatus.success,
          categories: state.categories
              .where((category) => category.id != categoryId)
              .toList(growable: false),
          selectedCategory: null,
          infoMessage: 'Category deleted.',
          clearError: true,
        ),
      );
    } catch (error) {
      _emitFailure(error, 'Unable to delete category.');
    }
  }

  Future<void> createItem(MerchantMenuItemRequest request) async {
    final restaurant = state.selectedRestaurant;
    final imageFile = state.menuItemImageFile;
    if (restaurant == null || state.isSaving) {
      return;
    }
    if (imageFile == null) {
      emit(
        state.copyWith(errorMessage: 'Please select an image for the menu item.'),
      );
      return;
    }
    emit(_saving());
    try {
      final item = await _catalogRepository.createMenuItem(
        restaurantId: restaurant.id,
        request: request,
        imageFile: imageFile,
      );
      emit(
        state.copyWith(
          status: MerchantCatalogStatus.success,
          items: _upsertItem(state.items, item),
          selectedItem: item,
          menuItemImageFile: null,
          infoMessage: 'Menu item created.',
          clearError: true,
        ),
      );
    } catch (error) {
      _emitFailure(error, 'Unable to create menu item.');
    }
  }

  Future<void> updateItem({
    required String menuItemId,
    required MerchantMenuItemRequest request,
  }) async {
    if (state.isSaving) {
      return;
    }
    emit(_saving());
    try {
      final item = await _catalogRepository.updateMenuItem(
        menuItemId: menuItemId,
        request: request,
      );
      emit(
        state.copyWith(
          status: MerchantCatalogStatus.success,
          items: _upsertItem(state.items, item),
          selectedItem: item,
          infoMessage: 'Menu item updated.',
          clearError: true,
        ),
      );
    } catch (error) {
      _emitFailure(error, 'Unable to update menu item.');
    }
  }

  Future<void> setItemAvailability({
    required String menuItemId,
    required bool isAvailable,
  }) async {
    if (state.isSaving) {
      return;
    }
    emit(_saving());
    try {
      final item = await _catalogRepository.updateMenuItemAvailability(
        menuItemId: menuItemId,
        isAvailable: isAvailable,
      );
      emit(
        state.copyWith(
          status: MerchantCatalogStatus.success,
          items: _upsertItem(state.items, item),
          selectedItem:
              state.selectedItem?.id == item.id ? item : state.selectedItem,
          infoMessage: item.available ? 'Item available.' : 'Item unavailable.',
          clearError: true,
        ),
      );
    } catch (error) {
      _emitFailure(error, 'Unable to update availability.');
    }
  }

  Future<void> deleteItem(String menuItemId) async {
    if (state.isSaving) {
      return;
    }
    emit(_saving());
    try {
      await _catalogRepository.deleteMenuItem(menuItemId);
      emit(
        state.copyWith(
          status: MerchantCatalogStatus.success,
          items: state.items
              .where((item) => item.id != menuItemId)
              .toList(growable: false),
          selectedItem: null,
          infoMessage: 'Menu item deleted.',
          clearError: true,
        ),
      );
    } catch (error) {
      _emitFailure(error, 'Unable to delete menu item.');
    }
  }

  void clearFeedback() {
    emit(state.copyWith(clearError: true, clearInfo: true));
  }

  MerchantCatalogState _saving() {
    return state.copyWith(
      status: MerchantCatalogStatus.saving,
      clearError: true,
      clearInfo: true,
    );
  }

  void _emitFailure(Object error, String fallback) {
    final presentation = ApiErrorUiMessageMapper.mapAny(
      error,
      fallback: fallback,
    );
    emit(
      state.copyWith(
        status: MerchantCatalogStatus.failure,
        errorMessage: presentation.message,
      ),
    );
  }

  List<MerchantMenuCategory> _upsertCategory(
    List<MerchantMenuCategory> categories,
    MerchantMenuCategory category,
  ) {
    final index = categories.indexWhere((item) => item.id == category.id);
    if (index == -1) {
      return [...categories, category]..sort((left, right) {
        final sort = left.sortOrder.compareTo(right.sortOrder);
        return sort == 0 ? left.name.compareTo(right.name) : sort;
      });
    }
    return [
      for (var i = 0; i < categories.length; i++)
        if (i == index) category else categories[i],
    ];
  }

  List<MerchantMenuItem> _upsertItem(
    List<MerchantMenuItem> items,
    MerchantMenuItem item,
  ) {
    final index = items.indexWhere((entry) => entry.id == item.id);
    if (index == -1) {
      return [...items, item];
    }
    return [
      for (var i = 0; i < items.length; i++)
        if (i == index) item else items[i],
    ];
  }
}
