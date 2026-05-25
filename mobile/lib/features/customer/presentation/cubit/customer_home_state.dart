import 'package:equatable/equatable.dart';

import '../../domain/models/restaurant_search_item.dart';

enum CustomerHomeStatus { initial, loading, success, failure }

class CustomerHomeState extends Equatable {
  const CustomerHomeState({
    required this.status,
    required this.isRefreshingLocation,
    required this.isNearbyLoading,
    required this.latitude,
    required this.longitude,
    required this.nearbyRestaurants,
    this.locationMessage,
    this.nearbyMessage,
    this.friendlyAddress,
  });

  const CustomerHomeState.initial()
    : this(
        status: CustomerHomeStatus.initial,
        isRefreshingLocation: false,
        isNearbyLoading: false,
        latitude: null,
        longitude: null,
        nearbyRestaurants: const [],
        locationMessage: null,
        nearbyMessage: null,
        friendlyAddress: null,
      );

  final CustomerHomeStatus status;
  final bool isRefreshingLocation;
  final bool isNearbyLoading;
  final double? latitude;
  final double? longitude;
  final List<RestaurantSearchItem> nearbyRestaurants;
  final String? locationMessage;
  final String? nearbyMessage;
  final String? friendlyAddress;

  String get locationLabel {
    if (friendlyAddress != null && friendlyAddress!.trim().isNotEmpty) {
      return friendlyAddress!;
    }

    if (latitude == null || longitude == null) {
      return locationMessage ?? 'Location unavailable';
    }

    return '${latitude!.toStringAsFixed(5)}, ${longitude!.toStringAsFixed(5)}';
  }

  CustomerHomeState copyWith({
    CustomerHomeStatus? status,
    bool? isRefreshingLocation,
    bool? isNearbyLoading,
    double? latitude,
    double? longitude,
    List<RestaurantSearchItem>? nearbyRestaurants,
    String? locationMessage,
    String? nearbyMessage,
    String? friendlyAddress,
    bool clearLocationMessage = false,
    bool clearNearbyMessage = false,
    bool clearCoordinates = false,
    bool clearFriendlyAddress = false,
  }) {
    return CustomerHomeState(
      status: status ?? this.status,
      isRefreshingLocation: isRefreshingLocation ?? this.isRefreshingLocation,
      isNearbyLoading: isNearbyLoading ?? this.isNearbyLoading,
      latitude: clearCoordinates ? null : (latitude ?? this.latitude),
      longitude: clearCoordinates ? null : (longitude ?? this.longitude),
      nearbyRestaurants: nearbyRestaurants ?? this.nearbyRestaurants,
      locationMessage:
          clearLocationMessage
              ? null
              : (locationMessage ?? this.locationMessage),
      nearbyMessage:
          clearNearbyMessage ? null : (nearbyMessage ?? this.nearbyMessage),
      friendlyAddress:
          clearFriendlyAddress
              ? null
              : (friendlyAddress ?? this.friendlyAddress),
    );
  }

  @override
  List<Object?> get props => [
    status,
    isRefreshingLocation,
    isNearbyLoading,
    latitude,
    longitude,
    nearbyRestaurants,
    locationMessage,
    nearbyMessage,
    friendlyAddress,
  ];
}
