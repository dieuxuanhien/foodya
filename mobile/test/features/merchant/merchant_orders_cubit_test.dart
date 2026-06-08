import 'package:flutter_test/flutter_test.dart';
import 'package:image_picker/image_picker.dart';
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
  _FakeMerchantRestaurantRepository({this.shouldFail = false});

  final bool shouldFail;

  @override
  Future<List<MerchantRestaurant>> listRestaurants({
    bool forceRefresh = false,
  }) async {
    if (shouldFail) {
      throw Exception('restaurants');
    }
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
    MerchantRestaurantRequest request, {
    XFile? backgroundFile,
    XFile? avatarFile,
  }) {
    throw UnimplementedError();
  }

  @override
  Future<MerchantRestaurant> updateRestaurant({
    required String restaurantId,
    required MerchantRestaurantRequest request,
    XFile? backgroundFile,
    XFile? avatarFile,
  }) {
    throw UnimplementedError();
  }
}

class _FakeMerchantOrderRepository implements MerchantOrderRepository {
  _FakeMerchantOrderRepository({this.shouldFail = false});

  final bool shouldFail;

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
    if (shouldFail) {
      throw Exception('orders');
    }
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

  test(
    'MerchantOrdersCubit reports restaurant and order load failures',
    () async {
      final restaurantFailureCubit = MerchantOrdersCubit(
        orderRepository: _FakeMerchantOrderRepository(),
        restaurantRepository: _FakeMerchantRestaurantRepository(
          shouldFail: true,
        ),
      );

      await restaurantFailureCubit.load();

      expect(restaurantFailureCubit.state.status, MerchantOrdersStatus.failure);
      expect(restaurantFailureCubit.state.errorMessage, isNotEmpty);

      final orderFailureCubit = MerchantOrdersCubit(
        orderRepository: _FakeMerchantOrderRepository(shouldFail: true),
        restaurantRepository: _FakeMerchantRestaurantRepository(),
      );

      await orderFailureCubit.load();

      expect(orderFailureCubit.state.status, MerchantOrdersStatus.failure);
      expect(orderFailureCubit.state.errorMessage, isNotEmpty);
    },
  );
}
