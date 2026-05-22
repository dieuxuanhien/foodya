import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:foodya_mobile/features/customer/domain/models/active_cart.dart';
import 'package:foodya_mobile/features/customer/domain/models/cart_item.dart';
import 'package:foodya_mobile/features/customer/domain/models/create_order_request.dart';
import 'package:foodya_mobile/features/customer/domain/models/order_cost_review.dart';
import 'package:foodya_mobile/features/customer/domain/models/order_created.dart';
import 'package:foodya_mobile/features/customer/domain/models/order_detail.dart';
import 'package:foodya_mobile/features/customer/domain/models/order_tracking_point.dart';
import 'package:foodya_mobile/features/customer/domain/models/order_summary.dart';
import 'package:foodya_mobile/features/customer/domain/repositories/customer_cart_repository.dart';
import 'package:foodya_mobile/features/customer/domain/repositories/customer_order_repository.dart';
import 'package:foodya_mobile/features/customer/presentation/cubit/cart_cubit.dart';
import 'package:foodya_mobile/features/customer/presentation/cubit/cart_state.dart';
import 'package:foodya_mobile/features/customer/presentation/cubit/order_list_cubit.dart';
import 'package:foodya_mobile/features/customer/presentation/cubit/order_list_state.dart';
import 'package:foodya_mobile/features/customer/presentation/pages/customer_cart_page.dart';

class _FakeCustomerCartRepository implements CustomerCartRepository {
  _FakeCustomerCartRepository(this.cart);

  ActiveCart cart;

  @override
  Future<ActiveCart> addItem({
    required String menuItemId,
    required int quantity,
    String? note,
  }) async {
    cart = ActiveCart(
      cartId: cart.cartId,
      restaurantId: cart.restaurantId,
      restaurantName: cart.restaurantName,
      subtotal: 1000.0,
      itemCount: 1,
      items: [
        CartItem(
          menuItemId: menuItemId,
          menuItemName: 'Item',
          unitPrice: 1000,
          quantity: quantity,
          lineTotal: 1000.0 * quantity,
          note: note,
        ),
      ],
    );
    return cart;
  }

  @override
  Future<ActiveCart> clearCart() async {
    cart = ActiveCart(
      cartId: cart.cartId,
      restaurantId: cart.restaurantId,
      restaurantName: cart.restaurantName,
      subtotal: 0.0,
      itemCount: 0,
      items: const [],
    );
    return cart;
  }

  @override
  Future<ActiveCart> getActiveCart() async => cart;

  @override
  Future<ActiveCart> removeItem(String menuItemId) async => cart;

  @override
  Future<ActiveCart> updateItem({
    required String menuItemId,
    required int quantity,
    String? note,
  }) async =>
      cart;
}

class _FakeCustomerOrderRepository implements CustomerOrderRepository {
  @override
  Future<OrderDetail> cancelOrder(String orderId, {String? reason}) {
    throw UnimplementedError();
  }

  @override
  Future<OrderCreated> createOrder({
    required CreateOrderRequest request,
    required String idempotencyKey,
  }) {
    throw UnimplementedError();
  }

  @override
  Future<OrderDetail> getOrderDetail(String orderId) {
    throw UnimplementedError();
  }

  @override
  Future<List<OrderTrackingPoint>> getTrackingPoints(String orderId) {
    throw UnimplementedError();
  }

  @override
  Future<List<OrderSummary>> listOrders() async {
    return [
      OrderSummary(
        orderId: '1',
        orderCode: 'FDY-001',
        customerName: 'Alice',
        restaurantName: 'Pho House',
        status: 'PENDING',
        paymentStatus: 'UNPAID',
        totalAmount: 45000.0,
      ),
    ];
  }

  @override
  Future<OrderCostReview> reviewOrderCost(CreateOrderRequest request) {
    throw UnimplementedError();
  }
}

void main() {
  testWidgets('Customer cart page shows empty cart state', (tester) async {
    final cartRepo = _FakeCustomerCartRepository(
      ActiveCart(
        cartId: 'cart-1',
        restaurantId: 'rest-1',
        restaurantName: 'Pho House',
        subtotal: 0.0,
        itemCount: 0,
        items: const [],
      ),
    );

    await tester.pumpWidget(
      RepositoryProvider<CustomerCartRepository>.value(
        value: cartRepo,
        child: const MaterialApp(home: CustomerCartPage()),
      ),
    );

    await tester.pumpAndSettle();

    expect(find.text('Your cart is empty'), findsOneWidget);
  });

  test('CartCubit loads cart state', () async {
    final cartRepo = _FakeCustomerCartRepository(
      ActiveCart(
        cartId: 'cart-1',
        restaurantId: 'rest-1',
        restaurantName: 'Pho House',
        subtotal: 0.0,
        itemCount: 0,
        items: const [],
      ),
    );
    final cubit = CartCubit(repository: cartRepo);

    await cubit.loadCart();

    expect(cubit.state.status, CartStatus.empty);
    expect(cubit.state.cart?.restaurantName, 'Pho House');
  });

  test('OrderListCubit loads order list', () async {
    final repo = _FakeCustomerOrderRepository();
    final cubit = OrderListCubit(repository: repo);

    await cubit.loadOrders();

    expect(cubit.state.status, OrderListStatus.success);
    expect(cubit.state.orders, hasLength(1));
  });
}
