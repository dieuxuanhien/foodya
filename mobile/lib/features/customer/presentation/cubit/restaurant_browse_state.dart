import 'package:equatable/equatable.dart';

import '../../domain/models/category_taxonomy.dart';
import '../../domain/models/restaurant_search_item.dart';

enum RestaurantBrowseStatus { initial, loading, success, empty, failure }

class RestaurantBrowseState extends Equatable {
  const RestaurantBrowseState({
    required this.status,
    required this.restaurants,
    required this.taxonomies,
    required this.selectedTaxonomyCodes,
    required this.keyword,
    required this.sort,
    required this.isNearby,
    required this.latitude,
    required this.longitude,
    required this.radiusKm,
    required this.openNow,
    required this.minRating,
    required this.page,
    required this.totalPages,
    required this.totalElements,
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
        isNearby: false,
        latitude: null,
        longitude: null,
        radiusKm: 5.0,
        openNow: null,
        minRating: null,
        page: 0,
        totalPages: 0,
        totalElements: 0,
        hasMore: false,
      );

  final RestaurantBrowseStatus status;
  final List<RestaurantSearchItem> restaurants;
  final List<CategoryTaxonomy> taxonomies;
  final List<String> selectedTaxonomyCodes;
  final String keyword;
  final String sort;
  final bool isNearby;
  final double? latitude;
  final double? longitude;
  final double radiusKm;
  final bool? openNow;
  final double? minRating;
  final int page;
  final int totalPages;
  final int totalElements;
  final bool hasMore;
  final String? errorMessage;

  bool get isInitialLoading =>
      status == RestaurantBrowseStatus.loading && restaurants.isEmpty;

  bool get isBusy => status == RestaurantBrowseStatus.loading;

  bool get canGoToPreviousPage => page > 0 && !isBusy;

  bool get canGoToNextPage => hasMore && !isBusy;

  int get pageNumber => restaurants.isEmpty ? 0 : page + 1;

  int get pageCount => totalPages;

  RestaurantBrowseState copyWith({
    RestaurantBrowseStatus? status,
    List<RestaurantSearchItem>? restaurants,
    List<CategoryTaxonomy>? taxonomies,
    List<String>? selectedTaxonomyCodes,
    String? keyword,
    String? sort,
    bool? isNearby,
    double? latitude,
    double? longitude,
    double? radiusKm,
    bool? openNow,
    double? minRating,
    int? page,
    int? totalPages,
    int? totalElements,
    bool? hasMore,
    String? errorMessage,
    bool clearError = false,
    bool resetOpenNow = false,
    bool resetMinRating = false,
    bool clearCoordinates = false,
  }) {
    return RestaurantBrowseState(
      status: status ?? this.status,
      restaurants: restaurants ?? this.restaurants,
      taxonomies: taxonomies ?? this.taxonomies,
      selectedTaxonomyCodes:
          selectedTaxonomyCodes ?? this.selectedTaxonomyCodes,
      keyword: keyword ?? this.keyword,
      sort: sort ?? this.sort,
      isNearby: isNearby ?? this.isNearby,
      latitude: clearCoordinates ? null : (latitude ?? this.latitude),
      longitude: clearCoordinates ? null : (longitude ?? this.longitude),
      radiusKm: radiusKm ?? this.radiusKm,
      openNow: resetOpenNow ? null : (openNow ?? this.openNow),
      minRating: resetMinRating ? null : (minRating ?? this.minRating),
      page: page ?? this.page,
      totalPages: totalPages ?? this.totalPages,
      totalElements: totalElements ?? this.totalElements,
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
    isNearby,
    latitude,
    longitude,
    radiusKm,
    openNow,
    minRating,
    page,
    totalPages,
    totalElements,
    hasMore,
    errorMessage,
  ];
}
