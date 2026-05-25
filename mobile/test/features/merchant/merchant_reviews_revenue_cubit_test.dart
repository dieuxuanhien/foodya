import 'package:flutter_test/flutter_test.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_restaurant.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_restaurant_request.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_revenue_report.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_review.dart';
import 'package:foodya_mobile/features/merchant/domain/repositories/merchant_restaurant_repository.dart';
import 'package:foodya_mobile/features/merchant/domain/repositories/merchant_revenue_repository.dart';
import 'package:foodya_mobile/features/merchant/domain/repositories/merchant_review_repository.dart';
import 'package:foodya_mobile/features/merchant/presentation/cubit/merchant_revenue_cubit.dart';
import 'package:foodya_mobile/features/merchant/presentation/cubit/merchant_revenue_state.dart';
import 'package:foodya_mobile/features/merchant/presentation/cubit/merchant_reviews_cubit.dart';
import 'package:foodya_mobile/features/merchant/presentation/cubit/merchant_reviews_state.dart';

class _FakeMerchantRestaurantRepository
    implements MerchantRestaurantRepository {
  @override
  Future<List<MerchantRestaurant>> listRestaurants() async {
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
    MerchantRestaurantRequest request,
  ) {
    throw UnimplementedError();
  }

  @override
  Future<MerchantRestaurant> updateRestaurant({
    required String restaurantId,
    required MerchantRestaurantRequest request,
  }) {
    throw UnimplementedError();
  }
}

class _FakeMerchantReviewRepository implements MerchantReviewRepository {
  MerchantReview review = MerchantReview(
    reviewId: 'review-1',
    orderId: 'order-1',
    restaurantId: 'restaurant-1',
    customerUserId: 'customer-1',
    stars: 5,
    comment: 'Great food',
    createdAt: DateTime(2026, 1, 1),
  );

  @override
  Future<List<MerchantReview>> listRestaurantReviews(
    String restaurantId,
  ) async {
    return [review];
  }

  @override
  Future<MerchantReview> respondToReview({
    required String reviewId,
    required String response,
  }) async {
    review = MerchantReview(
      reviewId: review.reviewId,
      orderId: review.orderId,
      restaurantId: review.restaurantId,
      customerUserId: review.customerUserId,
      stars: review.stars,
      comment: review.comment,
      merchantResponse: response,
      respondedAt: DateTime(2026, 1, 2),
      createdAt: review.createdAt,
    );
    return review;
  }
}

class _FakeMerchantRevenueRepository implements MerchantRevenueRepository {
  DateTime? lastFrom;
  DateTime? lastTo;
  int? lastTopItems;

  @override
  Future<MerchantRevenueReport> getRevenueReport({
    DateTime? from,
    DateTime? to,
    int topItems = 5,
  }) async {
    lastFrom = from;
    lastTo = to;
    lastTopItems = topItems;
    return MerchantRevenueReport(
      fromDate: DateTime(2026, 1, 1),
      toDate: DateTime(2026, 1, 31),
      revenue: 1000000,
      platformProfit: 120000,
      orderCount: 20,
      avgOrderValue: 50000,
      series: [
        MerchantRevenueBucket(
          period: DateTime(2026, 1, 1),
          revenue: 1000000,
          platformProfit: 120000,
          orderCount: 20,
          avgOrderValue: 50000,
        ),
      ],
      topSellingItems: const [
        MerchantTopSellingItem(
          menuItemId: 'item-1',
          itemName: 'Pho Bo',
          quantitySold: 12,
          revenue: 600000,
        ),
      ],
    );
  }
}

void main() {
  test('MerchantReviewsCubit loads reviews and saves response', () async {
    final cubit = MerchantReviewsCubit(
      reviewRepository: _FakeMerchantReviewRepository(),
      restaurantRepository: _FakeMerchantRestaurantRepository(),
    );

    await cubit.load();
    cubit.selectReview(cubit.state.reviews.single);
    await cubit.respond('Thanks for your feedback.');

    expect(cubit.state.status, MerchantReviewsStatus.success);
    expect(cubit.state.selectedRestaurant?.name, 'Pho House');
    expect(cubit.state.selectedReview?.merchantResponse, contains('Thanks'));
  });

  test('MerchantRevenueCubit loads report metrics', () async {
    final cubit = MerchantRevenueCubit(
      repository: _FakeMerchantRevenueRepository(),
    );

    await cubit.loadLast30Days();

    expect(cubit.state.status, MerchantRevenueStatus.success);
    expect(cubit.state.report?.orderCount, 20);
    expect(cubit.state.report?.topSellingItems.single.itemName, 'Pho Bo');
  });

  test('MerchantRevenueCubit applies custom filters', () async {
    final repo = _FakeMerchantRevenueRepository();
    final cubit = MerchantRevenueCubit(repository: repo);
    final from = DateTime(2026, 2, 1);
    final to = DateTime(2026, 2, 28);

    await cubit.setDateRange(from: from, to: to);
    await cubit.setTopItems(10);

    expect(cubit.state.status, MerchantRevenueStatus.success);
    expect(cubit.state.from, from);
    expect(cubit.state.to, to);
    expect(cubit.state.topItems, 10);
    expect(repo.lastFrom, from);
    expect(repo.lastTo, to);
    expect(repo.lastTopItems, 10);
  });
}
