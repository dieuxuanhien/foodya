import 'package:equatable/equatable.dart';

import '../../domain/models/category_taxonomy.dart';
import '../../domain/models/order_review.dart';
import '../../domain/models/restaurant_detail.dart';
import '../../domain/models/restaurant_menu_item.dart';

enum RestaurantDetailStatus {
  initial,
  loading,
  success,
  empty,
  loadingMore,
  failure,
}

class RestaurantDetailState extends Equatable {
  const RestaurantDetailState({
    required this.status,
    required this.restaurant,
    required this.menuItems,
    required this.reviews,
    required this.isReviewsLoading,
    required this.taxonomies,
    required this.selectedTaxonomyCodes,
    required this.menuKeyword,
    required this.menuSort,
    required this.page,
    required this.hasMore,
    this.errorMessage,
  });

  const RestaurantDetailState.initial()
    : this(
        status: RestaurantDetailStatus.initial,
        restaurant: null,
        menuItems: const [],
        reviews: const [],
        isReviewsLoading: false,
        taxonomies: const [],
        selectedTaxonomyCodes: const [],
        menuKeyword: '',
        menuSort: 'popularity_desc',
        page: 0,
        hasMore: false,
      );

  final RestaurantDetailStatus status;
  final RestaurantDetail? restaurant;
  final List<RestaurantMenuItem> menuItems;
  final List<OrderReview> reviews;
  final bool isReviewsLoading;
  final List<CategoryTaxonomy> taxonomies;
  final List<String> selectedTaxonomyCodes;
  final String menuKeyword;
  final String menuSort;
  final int page;
  final bool hasMore;
  final String? errorMessage;

  bool get isInitialLoading =>
      status == RestaurantDetailStatus.loading && restaurant == null;

  RestaurantDetailState copyWith({
    RestaurantDetailStatus? status,
    RestaurantDetail? restaurant,
    bool clearRestaurant = false,
    List<RestaurantMenuItem>? menuItems,
    List<OrderReview>? reviews,
    bool? isReviewsLoading,
    List<CategoryTaxonomy>? taxonomies,
    List<String>? selectedTaxonomyCodes,
    String? menuKeyword,
    String? menuSort,
    int? page,
    bool? hasMore,
    String? errorMessage,
    bool clearError = false,
  }) {
    return RestaurantDetailState(
      status: status ?? this.status,
      restaurant: clearRestaurant ? null : (restaurant ?? this.restaurant),
      menuItems: menuItems ?? this.menuItems,
      reviews: reviews ?? this.reviews,
      isReviewsLoading: isReviewsLoading ?? this.isReviewsLoading,
      taxonomies: taxonomies ?? this.taxonomies,
      selectedTaxonomyCodes:
          selectedTaxonomyCodes ?? this.selectedTaxonomyCodes,
      menuKeyword: menuKeyword ?? this.menuKeyword,
      menuSort: menuSort ?? this.menuSort,
      page: page ?? this.page,
      hasMore: hasMore ?? this.hasMore,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
    );
  }

  @override
  List<Object?> get props => [
    status,
    restaurant,
    menuItems,
    reviews,
    isReviewsLoading,
    taxonomies,
    selectedTaxonomyCodes,
    menuKeyword,
    menuSort,
    page,
    hasMore,
    errorMessage,
  ];
}
