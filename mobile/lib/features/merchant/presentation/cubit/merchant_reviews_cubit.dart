import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/network/api_error_ui_message.dart';
import '../../domain/models/merchant_restaurant.dart';
import '../../domain/models/merchant_review.dart';
import '../../domain/repositories/merchant_restaurant_repository.dart';
import '../../domain/repositories/merchant_review_repository.dart';
import 'merchant_restaurant_selection_cubit.dart';
import 'merchant_reviews_state.dart';

class MerchantReviewsCubit extends Cubit<MerchantReviewsState> {
  MerchantReviewsCubit({
    required MerchantReviewRepository reviewRepository,
    required MerchantRestaurantRepository restaurantRepository,
    MerchantRestaurantSelectionCubit? selectionCubit,
  }) : _reviewRepository = reviewRepository,
       _restaurantRepository = restaurantRepository,
       _selectionCubit = selectionCubit,
       super(const MerchantReviewsState.initial());

  final MerchantReviewRepository _reviewRepository;
  final MerchantRestaurantRepository _restaurantRepository;
  final MerchantRestaurantSelectionCubit? _selectionCubit;

  Future<void> load() async {
    if (state.isBusy) {
      return;
    }
    emit(
      state.copyWith(
        status: MerchantReviewsStatus.loading,
        clearError: true,
        clearInfo: true,
      ),
    );
    try {
      final restaurants = await _restaurantRepository.listRestaurants();
      final selected = _resolveSelectedRestaurant(restaurants);
      emit(
        state.copyWith(
          status: MerchantReviewsStatus.success,
          restaurants: restaurants,
          selectedRestaurant: selected,
          clearError: true,
        ),
      );
      if (selected != null) {
        await loadRestaurantReviews(selected, clearSelection: true);
      }
    } catch (error) {
      _emitFailure(error, 'Unable to load reviews.');
    }
  }

  Future<void> loadRestaurantReviews(
    MerchantRestaurant restaurant, {
    bool clearSelection = false,
  }) async {
    _selectionCubit?.selectRestaurant(restaurant);
    emit(
      state.copyWith(
        status: MerchantReviewsStatus.loading,
        selectedRestaurant: restaurant,
        selectedReview: clearSelection ? null : state.selectedReview,
        clearError: true,
        clearInfo: true,
      ),
    );
    try {
      final reviews = await _reviewRepository.listRestaurantReviews(
        restaurant.id,
      );
      emit(
        state.copyWith(
          status: MerchantReviewsStatus.success,
          reviews: reviews,
          selectedReview: clearSelection ? null : state.selectedReview,
          clearError: true,
        ),
      );
    } catch (error) {
      _emitFailure(error, 'Unable to load restaurant reviews.');
    }
  }

  void selectReview(MerchantReview review) {
    emit(state.copyWith(selectedReview: review, clearError: true));
  }

  Future<void> respond(String response) async {
    final review = state.selectedReview;
    if (review == null || state.isSaving) {
      return;
    }
    emit(
      state.copyWith(
        status: MerchantReviewsStatus.saving,
        clearError: true,
        clearInfo: true,
      ),
    );
    try {
      final updated = await _reviewRepository.respondToReview(
        reviewId: review.reviewId,
        response: response,
      );
      emit(
        state.copyWith(
          status: MerchantReviewsStatus.success,
          reviews: _upsert(state.reviews, updated),
          selectedReview: updated,
          infoMessage: 'Review response saved.',
          clearError: true,
        ),
      );
    } catch (error) {
      _emitFailure(error, 'Unable to save review response.');
    }
  }

  void clearFeedback() {
    emit(state.copyWith(clearError: true, clearInfo: true));
  }

  List<MerchantReview> _upsert(
    List<MerchantReview> reviews,
    MerchantReview review,
  ) {
    final index = reviews.indexWhere(
      (item) => item.reviewId == review.reviewId,
    );
    if (index == -1) {
      return [review, ...reviews];
    }
    return [
      for (var i = 0; i < reviews.length; i++)
        if (i == index) review else reviews[i],
    ];
  }

  void _emitFailure(Object error, String fallback) {
    final presentation = ApiErrorUiMessageMapper.mapAny(
      error,
      fallback: fallback,
    );
    emit(
      state.copyWith(
        status: MerchantReviewsStatus.failure,
        errorMessage: presentation.message,
      ),
    );
  }

  MerchantRestaurant? _resolveSelectedRestaurant(
    List<MerchantRestaurant> restaurants,
  ) {
    if (restaurants.isEmpty) {
      _selectionCubit?.selectRestaurantId(null);
      return null;
    }
    final selectedId = _selectionCubit?.state.restaurantId;
    if (selectedId != null) {
      final selected = _findRestaurant(restaurants, selectedId);
      if (selected != null) {
        return selected;
      }
    }
    final current = state.selectedRestaurant;
    if (current != null) {
      final selected = _findRestaurant(restaurants, current.id);
      if (selected != null) {
        return selected;
      }
    }
    final selected = restaurants.first;
    _selectionCubit?.selectRestaurant(selected);
    return selected;
  }

  MerchantRestaurant? _findRestaurant(
    List<MerchantRestaurant> restaurants,
    String restaurantId,
  ) {
    for (final restaurant in restaurants) {
      if (restaurant.id == restaurantId) {
        return restaurant;
      }
    }
    return null;
  }
}
