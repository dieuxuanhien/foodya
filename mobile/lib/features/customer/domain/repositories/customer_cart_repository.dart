import '../models/active_cart.dart';

abstract class CustomerCartRepository {
  Future<ActiveCart> getActiveCart();

  Future<ActiveCart> addItem({
    required String menuItemId,
    required int quantity,
    String? note,
  });

  Future<ActiveCart> updateItem({
    required String menuItemId,
    required int quantity,
    String? note,
  });

  Future<ActiveCart> removeItem(String menuItemId);

  Future<ActiveCart> clearCart();
}
