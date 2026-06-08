import 'package:equatable/equatable.dart';

import '../../domain/models/merchant_restaurant.dart';
import '../../domain/models/merchant_review.dart';

enum MerchantReviewsStatus { initial, loading, saving, success, failure }

const Object _unset = Object();

class MerchantReviewsState extends Equatable {
  const MerchantReviewsState({
    required this.status,
    this.restaurants = const [],
    this.reviews = const [],
    this.selectedRestaurant,
    this.selectedReview,
    this.errorMessage,
    this.infoMessage,
  });

  const MerchantReviewsState.initial()
    : this(status: MerchantReviewsStatus.initial);

  final MerchantReviewsStatus status;
  final List<MerchantRestaurant> restaurants;
  final List<MerchantReview> reviews;
  final MerchantRestaurant? selectedRestaurant;
  final MerchantReview? selectedReview;
  final String? errorMessage;
  final String? infoMessage;

  bool get isLoading => status == MerchantReviewsStatus.loading;
  bool get isSaving => status == MerchantReviewsStatus.saving;
  bool get isBusy => isLoading || isSaving;

  MerchantReviewsState copyWith({
    MerchantReviewsStatus? status,
    List<MerchantRestaurant>? restaurants,
    List<MerchantReview>? reviews,
    Object? selectedRestaurant = _unset,
    Object? selectedReview = _unset,
    String? errorMessage,
    String? infoMessage,
    bool clearError = false,
    bool clearInfo = false,
  }) {
    return MerchantReviewsState(
      status: status ?? this.status,
      restaurants: restaurants ?? this.restaurants,
      reviews: reviews ?? this.reviews,
      selectedRestaurant:
          selectedRestaurant == _unset
              ? this.selectedRestaurant
              : selectedRestaurant as MerchantRestaurant?,
      selectedReview:
          selectedReview == _unset
              ? this.selectedReview
              : selectedReview as MerchantReview?,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      infoMessage: clearInfo ? null : (infoMessage ?? this.infoMessage),
    );
  }

  @override
  List<Object?> get props => [
    status,
    restaurants,
    reviews,
    selectedRestaurant,
    selectedReview,
    errorMessage,
    infoMessage,
  ];
}
