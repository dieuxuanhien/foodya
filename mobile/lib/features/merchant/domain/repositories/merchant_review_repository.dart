import '../models/merchant_review.dart';

abstract class MerchantReviewRepository {
  Future<List<MerchantReview>> listRestaurantReviews(String restaurantId);

  Future<MerchantReview> respondToReview({
    required String reviewId,
    required String response,
  });
}
