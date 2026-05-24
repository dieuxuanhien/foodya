import 'package:flutter_test/flutter_test.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_order_detail.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_order_summary.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_restaurant.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_restaurant_request.dart';
import 'package:foodya_mobile/features/merchant/domain/repositories/merchant_order_repository.dart';
import 'package:foodya_mobile/features/merchant/domain/repositories/merchant_restaurant_repository.dart';
import 'package:foodya_mobile/features/merchant/presentation/cubit/merchant_orders_cubit.dart';
import 'package:foodya_mobile/features/merchant/presentation/cubit/merchant_orders_state.dart';

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

class _FakeMerchantOrderRepository implements MerchantOrderRepository {
  MerchantOrderDetail detail = const MerchantOrderDetail(
    orderId: 'order-1',
    orderCode: 'FDY-001',
    restaurantId: 'restaurant-1',
    restaurantName: 'Pho House',
    customerUserId: 'customer-1',
    customerName: 'Alice',
    status: 'PENDING',
    paymentMethod: 'COD',
    paymentStatus: 'UNPAID',
    subtotalAmount: 45000,
    deliveryFee: 10000,
    totalAmount: 55000,
    deliveryAddress: '1 Le Loi',
  );

  @override
  Future<List<MerchantOrderSummary>> listRestaurantOrders(
    String restaurantId,
  ) async {
    return [
      MerchantOrderSummary(
        orderId: detail.orderId,
        orderCode: detail.orderCode,
        customerName: detail.customerName,
        restaurantName: detail.restaurantName,
        status: detail.status,
        paymentStatus: detail.paymentStatus,
        totalAmount: detail.totalAmount,
      ),
    ];
  }

  @override
  Future<MerchantOrderDetail> getOrderDetail(String orderId) async => detail;

  @override
  Future<MerchantOrderDetail> updateOrderStatus({
    required String orderId,
    required String status,
  }) async {
    detail = MerchantOrderDetail(
      orderId: detail.orderId,
      orderCode: detail.orderCode,
      restaurantId: detail.restaurantId,
      restaurantName: detail.restaurantName,
      customerUserId: detail.customerUserId,
      customerName: detail.customerName,
      status: status,
      paymentMethod: detail.paymentMethod,
      paymentStatus: detail.paymentStatus,
      subtotalAmount: detail.subtotalAmount,
      deliveryFee: detail.deliveryFee,
      totalAmount: detail.totalAmount,
      deliveryAddress: detail.deliveryAddress,
    );
    return detail;
  }
}

void main() {
  test('MerchantOrdersCubit loads restaurant orders', () async {
    final cubit = MerchantOrdersCubit(
      orderRepository: _FakeMerchantOrderRepository(),
      restaurantRepository: _FakeMerchantRestaurantRepository(),
    );

    await cubit.load();

    expect(cubit.state.status, MerchantOrdersStatus.success);
    expect(cubit.state.selectedRestaurant?.name, 'Pho House');
    expect(cubit.state.orders.single.orderCode, 'FDY-001');
  });

  test('MerchantOrdersCubit selects and accepts an order', () async {
    final cubit = MerchantOrdersCubit(
      orderRepository: _FakeMerchantOrderRepository(),
      restaurantRepository: _FakeMerchantRestaurantRepository(),
    );

    await cubit.load();
    await cubit.selectOrder(cubit.state.orders.single);
    await cubit.updateStatus('ACCEPTED');

    expect(cubit.state.status, MerchantOrdersStatus.success);
    expect(cubit.state.selectedOrder?.status, 'ACCEPTED');
    expect(cubit.state.orders.single.status, 'ACCEPTED');
  });
}
