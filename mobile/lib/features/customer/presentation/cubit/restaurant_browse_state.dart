import 'package:equatable/equatable.dart';

import '../../domain/models/category_taxonomy.dart';
import '../../domain/models/restaurant_search_item.dart';

enum RestaurantBrowseStatus {
  initial,
  loading,
  success,
  empty,
  loadingMore,
  failure,
}

class RestaurantBrowseState extends Equatable {
  const RestaurantBrowseState({
    required this.status,
    required this.restaurants,
    required this.taxonomies,
    required this.selectedTaxonomyCodes,
    required this.keyword,
    required this.sort,
    required this.openNow,
    required this.minRating,
    required this.page,
    required this.hasMore,
    this.errorMessage,
  });

  const RestaurantBrowseState.initial()
    : this(
        status: RestaurantBrowseStatus.initial,
        restaurants: const [],
        taxonomies: const [],
        selectedTaxonomyCodes: const [],
        keyword: '',
        sort: 'relevance',
        openNow: null,
        minRating: null,
        page: 0,
        hasMore: false,
      );

  final RestaurantBrowseStatus status;
  final List<RestaurantSearchItem> restaurants;
  final List<CategoryTaxonomy> taxonomies;
  final List<String> selectedTaxonomyCodes;
  final String keyword;
  final String sort;
  final bool? openNow;
  final double? minRating;
  final int page;
  final bool hasMore;
  final String? errorMessage;

  bool get isInitialLoading =>
      status == RestaurantBrowseStatus.loading && restaurants.isEmpty;

  bool get isBusy =>
      status == RestaurantBrowseStatus.loading ||
      status == RestaurantBrowseStatus.loadingMore;

  RestaurantBrowseState copyWith({
    RestaurantBrowseStatus? status,
    List<RestaurantSearchItem>? restaurants,
    List<CategoryTaxonomy>? taxonomies,
    List<String>? selectedTaxonomyCodes,
    String? keyword,
    String? sort,
    bool? openNow,
    double? minRating,
    int? page,
    bool? hasMore,
    String? errorMessage,
    bool clearError = false,
    bool resetOpenNow = false,
    bool resetMinRating = false,
  }) {
    return RestaurantBrowseState(
      status: status ?? this.status,
      restaurants: restaurants ?? this.restaurants,
      taxonomies: taxonomies ?? this.taxonomies,
      selectedTaxonomyCodes:
          selectedTaxonomyCodes ?? this.selectedTaxonomyCodes,
      keyword: keyword ?? this.keyword,
      sort: sort ?? this.sort,
      openNow: resetOpenNow ? null : (openNow ?? this.openNow),
      minRating: resetMinRating ? null : (minRating ?? this.minRating),
      page: page ?? this.page,
      hasMore: hasMore ?? this.hasMore,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
    );
  }

  @override
  List<Object?> get props => [
    status,
    restaurants,
    taxonomies,
    selectedTaxonomyCodes,
    keyword,
    sort,
    openNow,
    minRating,
    page,
    hasMore,
    errorMessage,
  ];
}
