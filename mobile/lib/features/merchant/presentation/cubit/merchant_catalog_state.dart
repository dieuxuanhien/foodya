import 'package:equatable/equatable.dart';
import 'package:image_picker/image_picker.dart';

import '../../domain/models/merchant_category_taxonomy.dart';
import '../../domain/models/merchant_menu_category.dart';
import '../../domain/models/merchant_menu_item.dart';
import '../../domain/models/merchant_restaurant.dart';

enum MerchantCatalogStatus { initial, loading, saving, success, failure }

const Object _unset = Object();

class MerchantCatalogState extends Equatable {
  const MerchantCatalogState({
    required this.status,
    this.restaurants = const [],
    this.categories = const [],
    this.items = const [],
    this.taxonomies = const [],
    this.selectedRestaurant,
    this.selectedCategory,
    this.selectedItem,
    this.menuItemImageFile,
    this.errorMessage,
    this.infoMessage,
  });

  const MerchantCatalogState.initial()
    : this(status: MerchantCatalogStatus.initial);

  final MerchantCatalogStatus status;
  final List<MerchantRestaurant> restaurants;
  final List<MerchantMenuCategory> categories;
  final List<MerchantMenuItem> items;
  final List<MerchantCategoryTaxonomy> taxonomies;
  final MerchantRestaurant? selectedRestaurant;
  final MerchantMenuCategory? selectedCategory;
  final MerchantMenuItem? selectedItem;
  final XFile? menuItemImageFile;
  final String? errorMessage;
  final String? infoMessage;

  bool get isLoading => status == MerchantCatalogStatus.loading;
  bool get isSaving => status == MerchantCatalogStatus.saving;
  bool get isBusy => isLoading || isSaving;

  MerchantCatalogState copyWith({
    MerchantCatalogStatus? status,
    List<MerchantRestaurant>? restaurants,
    List<MerchantMenuCategory>? categories,
    List<MerchantMenuItem>? items,
    List<MerchantCategoryTaxonomy>? taxonomies,
    Object? selectedRestaurant = _unset,
    Object? selectedCategory = _unset,
    Object? selectedItem = _unset,
    Object? menuItemImageFile = _unset,
    String? errorMessage,
    String? infoMessage,
    bool clearError = false,
    bool clearInfo = false,
  }) {
    return MerchantCatalogState(
      status: status ?? this.status,
      restaurants: restaurants ?? this.restaurants,
      categories: categories ?? this.categories,
      items: items ?? this.items,
      taxonomies: taxonomies ?? this.taxonomies,
      selectedRestaurant:
          selectedRestaurant == _unset
              ? this.selectedRestaurant
              : selectedRestaurant as MerchantRestaurant?,
      selectedCategory:
          selectedCategory == _unset
              ? this.selectedCategory
              : selectedCategory as MerchantMenuCategory?,
      selectedItem:
          selectedItem == _unset
              ? this.selectedItem
              : selectedItem as MerchantMenuItem?,
      menuItemImageFile:
          menuItemImageFile == _unset
              ? this.menuItemImageFile
              : menuItemImageFile as XFile?,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      infoMessage: clearInfo ? null : (infoMessage ?? this.infoMessage),
    );
  }

  @override
  List<Object?> get props => [
    status,
    restaurants,
    categories,
    items,
    taxonomies,
    selectedRestaurant,
    selectedCategory,
    selectedItem,
    menuItemImageFile?.path,
    errorMessage,
    infoMessage,
  ];
}
