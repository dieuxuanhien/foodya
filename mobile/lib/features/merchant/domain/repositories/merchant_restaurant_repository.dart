import '../models/merchant_restaurant.dart';
import '../models/merchant_restaurant_request.dart';

abstract class MerchantRestaurantRepository {
  Future<List<MerchantRestaurant>> listRestaurants();

  Future<MerchantRestaurant> createRestaurant(
    MerchantRestaurantRequest request,
  );

  Future<MerchantRestaurant> updateRestaurant({
    required String restaurantId,
    required MerchantRestaurantRequest request,
  });
}
