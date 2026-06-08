import 'package:image_picker/image_picker.dart';

import '../models/merchant_restaurant.dart';
import '../models/merchant_restaurant_request.dart';

abstract class MerchantRestaurantRepository {
  Future<List<MerchantRestaurant>> listRestaurants();

  Future<MerchantRestaurant> createRestaurant(
    MerchantRestaurantRequest request, {
    XFile? backgroundFile,
    XFile? avatarFile,
  });

  Future<MerchantRestaurant> updateRestaurant({
    required String restaurantId,
    required MerchantRestaurantRequest request,
    XFile? backgroundFile,
    XFile? avatarFile,
  });
}
