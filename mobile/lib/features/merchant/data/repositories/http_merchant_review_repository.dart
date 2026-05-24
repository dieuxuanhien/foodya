import '../../../../core/auth/auth_session_recovery.dart';
import '../../domain/models/merchant_review.dart';
import '../../domain/repositories/merchant_review_repository.dart';
import '../data_sources/merchant_review_remote_data_source.dart';

class HttpMerchantReviewRepository implements MerchantReviewRepository {
  HttpMerchantReviewRepository({
    required MerchantReviewRemoteDataSource remoteDataSource,
    required AuthSessionRecovery sessionRecovery,
  }) : _remoteDataSource = remoteDataSource,
       _sessionRecovery = sessionRecovery;

  final MerchantReviewRemoteDataSource _remoteDataSource;
  final AuthSessionRecovery _sessionRecovery;

  @override
  Future<List<MerchantReview>> listRestaurantReviews(String restaurantId) {
    return _remoteDataSource.listRestaurantReviews(restaurantId: restaurantId);
  }

  @override
  Future<MerchantReview> respondToReview({
    required String reviewId,
    required String response,
  }) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.respondToReview(
        accessToken: accessToken,
        reviewId: reviewId,
        response: response,
      );
    });
  }
}
