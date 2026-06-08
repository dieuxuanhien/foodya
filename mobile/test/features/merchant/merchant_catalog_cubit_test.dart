import 'package:flutter_test/flutter_test.dart';
import 'package:image_picker/image_picker.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_category_taxonomy.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_menu_category.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_menu_category_request.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_menu_item.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_menu_item_request.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_restaurant.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_restaurant_request.dart';
import 'package:foodya_mobile/features/merchant/domain/repositories/merchant_catalog_repository.dart';
import 'package:foodya_mobile/features/merchant/domain/repositories/merchant_restaurant_repository.dart';
import 'package:foodya_mobile/features/merchant/presentation/cubit/merchant_catalog_cubit.dart';
import 'package:foodya_mobile/features/merchant/presentation/cubit/merchant_catalog_state.dart';

class _FakeMerchantRestaurantRepository
    implements MerchantRestaurantRepository {
  @override
  Future<List<MerchantRestaurant>> listRestaurants({
    bool forceRefresh = false,
  }) async {
    return const [
      MerchantRestaurant(
        id: 'restaurant-1',
        name: 'Pho House',
        cuisineType: 'Vietnamese',
        description: 'Soup',
        addressLine: '1 Nguyen Hue',
        latitude: 10.77,
        longitude: 106.7,
        avgRating: 4.5,
        reviewCount: 10,
        status: 'APPROVED',
        open: true,
        maxDeliveryKm: 5,
      ),
    ];
  }

  @override
  Future<MerchantRestaurant> createRestaurant(
    MerchantRestaurantRequest request, {
    XFile? backgroundFile,
    XFile? avatarFile,
  }) {
    throw UnimplementedError();
  }

  @override
  Future<MerchantRestaurant> updateRestaurant({
    required String restaurantId,
    required MerchantRestaurantRequest request,
    XFile? backgroundFile,
    XFile? avatarFile,
  }) {
    throw UnimplementedError();
  }
}

class _FakeMerchantCatalogRepository implements MerchantCatalogRepository {
  _FakeMerchantCatalogRepository({this.shouldFail = false});

  final bool shouldFail;

  MerchantMenuItem item = const MerchantMenuItem(
    id: 'item-1',
    restaurantId: 'restaurant-1',
    restaurantName: 'Pho House',
    categoryId: 'category-1',
    categoryName: 'Noodles',
    taxonomyCodes: ['NOODLES'],
    name: 'Pho Bo',
    description: 'Beef noodle soup',
    price: 65000,
    active: true,
    available: true,
  );

  @override
  Future<List<MerchantCategoryTaxonomy>> listCategoryTaxonomies() async {
    if (shouldFail) {
      throw Exception('taxonomies');
    }
    return const [
      MerchantCategoryTaxonomy(
        code: 'NOODLES',
        displayName: 'Noodles',
        sortOrder: 1,
        active: true,
      ),
    ];
  }

  @override
  Future<List<MerchantMenuCategory>> listCategories(String restaurantId) async {
    return const [
      MerchantMenuCategory(
        id: 'category-1',
        restaurantId: 'restaurant-1',
        name: 'Noodles',
        sortOrder: 1,
        active: true,
      ),
    ];
  }

  @override
  Future<List<MerchantMenuItem>> listMenuItems(String restaurantId) async {
    return [item];
  }

  @override
  Future<MerchantMenuItem> updateMenuItemAvailability({
    required String menuItemId,
    required bool isAvailable,
  }) async {
    item = MerchantMenuItem(
      id: item.id,
      restaurantId: item.restaurantId,
      restaurantName: item.restaurantName,
      categoryId: item.categoryId,
      categoryName: item.categoryName,
      taxonomyCodes: item.taxonomyCodes,
      name: item.name,
      description: item.description,
      price: item.price,
      active: item.active,
      available: isAvailable,
    );
    return item;
  }

  @override
  Future<MerchantMenuCategory> createCategory({
    required String restaurantId,
    required MerchantMenuCategoryRequest request,
  }) {
    throw UnimplementedError();
  }

  @override
  Future<MerchantMenuCategory> updateCategory({
    required String categoryId,
    required MerchantMenuCategoryRequest request,
  }) {
    throw UnimplementedError();
  }

  @override
  Future<void> deleteCategory(String categoryId) {
    throw UnimplementedError();
  }

  @override
  Future<MerchantMenuItem> createMenuItem({
    required String restaurantId,
    required MerchantMenuItemRequest request,
    required XFile imageFile,
  }) {
    throw UnimplementedError();
  }

  @override
  Future<MerchantMenuItem> updateMenuItem({
    required String menuItemId,
    required MerchantMenuItemRequest request,
  }) {
    throw UnimplementedError();
  }

  @override
  Future<void> deleteMenuItem(String menuItemId) {
    throw UnimplementedError();
  }
}

void main() {
  test(
    'MerchantCatalogCubit loads restaurants, categories, and items',
    () async {
      final cubit = MerchantCatalogCubit(
        catalogRepository: _FakeMerchantCatalogRepository(),
        restaurantRepository: _FakeMerchantRestaurantRepository(),
      );

      await cubit.load();

      expect(cubit.state.status, MerchantCatalogStatus.success);
      expect(cubit.state.selectedRestaurant?.name, 'Pho House');
      expect(cubit.state.categories.single.name, 'Noodles');
      expect(cubit.state.items.single.name, 'Pho Bo');
    },
  );

  test('MerchantCatalogCubit updates menu item availability', () async {
    final catalogRepository = _FakeMerchantCatalogRepository();
    final cubit = MerchantCatalogCubit(
      catalogRepository: catalogRepository,
      restaurantRepository: _FakeMerchantRestaurantRepository(),
    );

    await cubit.load();
    await cubit.setItemAvailability(menuItemId: 'item-1', isAvailable: false);

    expect(cubit.state.status, MerchantCatalogStatus.success);
    expect(cubit.state.items.single.available, isFalse);
  });

  test(
    'MerchantCatalogCubit reports empty restaurants and load failure',
    () async {
      final emptyCubit = MerchantCatalogCubit(
        catalogRepository: _FakeMerchantCatalogRepository(),
        restaurantRepository: _EmptyMerchantRestaurantRepository(),
      );

      await emptyCubit.load();

      expect(emptyCubit.state.status, MerchantCatalogStatus.success);
      expect(emptyCubit.state.selectedRestaurant, isNull);

      final failureCubit = MerchantCatalogCubit(
        catalogRepository: _FakeMerchantCatalogRepository(shouldFail: true),
        restaurantRepository: _FakeMerchantRestaurantRepository(),
      );

      await failureCubit.load();

      expect(failureCubit.state.status, MerchantCatalogStatus.failure);
      expect(failureCubit.state.errorMessage, isNotEmpty);
    },
  );
}

class _EmptyMerchantRestaurantRepository
    implements MerchantRestaurantRepository {
  @override
  Future<List<MerchantRestaurant>> listRestaurants({
    bool forceRefresh = false,
  }) async => const [];

  @override
  Future<MerchantRestaurant> createRestaurant(
    MerchantRestaurantRequest request, {
    XFile? backgroundFile,
    XFile? avatarFile,
  }) {
    throw UnimplementedError();
  }

  @override
  Future<MerchantRestaurant> updateRestaurant({
    required String restaurantId,
    required MerchantRestaurantRequest request,
    XFile? backgroundFile,
    XFile? avatarFile,
  }) {
    throw UnimplementedError();
  }
}
