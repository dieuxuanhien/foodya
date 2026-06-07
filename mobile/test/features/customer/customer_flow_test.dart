import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:foodya_mobile/features/customer/domain/models/active_cart.dart';
import 'package:foodya_mobile/features/customer/domain/models/cart_item.dart';
import 'package:foodya_mobile/features/customer/domain/models/create_order_request.dart';
import 'package:foodya_mobile/features/customer/domain/models/order_cost_review.dart';
import 'package:foodya_mobile/features/customer/domain/models/order_created.dart';
import 'package:foodya_mobile/features/customer/domain/models/order_detail.dart';
import 'package:foodya_mobile/features/customer/domain/models/order_review.dart';
import 'package:foodya_mobile/features/customer/domain/models/order_tracking_point.dart';
import 'package:foodya_mobile/features/customer/domain/models/order_summary.dart';
import 'package:foodya_mobile/features/customer/domain/repositories/customer_cart_repository.dart';
import 'package:foodya_mobile/features/customer/domain/repositories/customer_order_repository.dart';
import 'package:foodya_mobile/core/realtime/order_tracking_realtime_service.dart';
import 'package:foodya_mobile/features/customer/presentation/cubit/cart_cubit.dart';
import 'package:foodya_mobile/features/customer/presentation/cubit/cart_state.dart';
import 'package:foodya_mobile/features/customer/presentation/cubit/order_detail_cubit.dart';
import 'package:foodya_mobile/features/customer/presentation/cubit/order_detail_state.dart';
import 'package:foodya_mobile/features/customer/presentation/cubit/order_list_cubit.dart';
import 'package:foodya_mobile/features/customer/presentation/cubit/order_list_state.dart';
import 'package:foodya_mobile/features/customer/presentation/pages/customer_cart_page.dart';
import 'package:foodya_mobile/features/customer/presentation/pages/customer_order_detail_page.dart';

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
  }) async => cart;
}

class _FakeCustomerOrderRepository implements CustomerOrderRepository {
  _FakeCustomerOrderRepository({this.detailStatus = 'DELIVERING'});

  final String detailStatus;
  int detailLoads = 0;
  int trackingLoads = 0;

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
  Future<OrderDetail> getOrderDetail(String orderId) async {
    detailLoads++;
    return OrderDetail(
      orderId: orderId,
      orderCode: 'FDY-001',
      restaurantId: 'rest-1',
      restaurantName: 'Pho House',
      customerUserId: 'customer-1',
      customerName: 'Alice',
      status: detailStatus,
      paymentMethod: 'COD',
      paymentStatus: 'UNPAID',
      subtotalAmount: 40000.0,
      deliveryFee: 5000.0,
      totalAmount: 45000.0,
      deliveryAddress: '1 Nguyen Trai',
      deliveryLatitude: 10.765,
      deliveryLongitude: 106.664,
    );
  }

  @override
  Future<List<OrderTrackingPoint>> getTrackingPoints(String orderId) async {
    trackingLoads++;
    return [
      OrderTrackingPoint(
        lat: 10.762622,
        lng: 106.660172,
        recordedAt: DateTime.utc(2026, 1, 1, 10),
      ),
      OrderTrackingPoint(
        lat: 10.763,
        lng: 106.662,
        recordedAt: DateTime.utc(2026, 1, 1, 10, 1),
      ),
    ];
  }

  @override
  Future<OrderReview> createReview({
    required String orderId,
    required int stars,
    String? comment,
  }) {
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

class _FakeOrderTrackingRealtimeService
    implements OrderTrackingRealtimeService {
  final _controller = StreamController<OrderTrackingRealtimeUpdate>.broadcast();
  var disposed = false;

  @override
  Stream<OrderTrackingRealtimeUpdate> watchOrderTracking(String orderId) {
    return _controller.stream;
  }

  void emit(OrderTrackingRealtimeUpdate update) {
    _controller.add(update);
  }

  @override
  Future<void> dispose() async {
    disposed = true;
    await _controller.close();
  }
}

void main() {
  test('OrderDetail parses delivery coordinates from JSON', () {
    final detail = OrderDetail.fromJson(const {
      'orderId': 'order-1',
      'orderCode': 'FDY-001',
      'restaurantId': 'rest-1',
      'restaurantName': 'Pho House',
      'customerUserId': 'customer-1',
      'customerName': 'Alice',
      'status': 'DELIVERING',
      'paymentMethod': 'COD',
      'paymentStatus': 'UNPAID',
      'subtotalAmount': 40000,
      'deliveryFee': 5000,
      'totalAmount': 45000,
      'deliveryAddress': '1 Nguyen Trai',
      'deliveryLatitude': '10.765',
      'deliveryLongitude': 106.664,
    });

    expect(detail.deliveryLatitude, 10.765);
    expect(detail.deliveryLongitude, 106.664);
  });

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

  test('OrderDetailCubit starts live tracking for delivering order', () async {
    final repo = _FakeCustomerOrderRepository();
    final cubit = OrderDetailCubit(repository: repo);

    await cubit.load('order-1');

    expect(cubit.state.status, OrderDetailStatus.success);
    expect(cubit.state.order?.status, 'DELIVERING');
    expect(cubit.state.trackingPoints, hasLength(2));
    expect(cubit.state.isLiveTrackingEnabled, isTrue);
    expect(cubit.state.lastTrackingRefreshAt, isNotNull);
    expect(repo.detailLoads, 2);
    expect(repo.trackingLoads, 1);

    await cubit.close();
  });

  for (final status in ['ASSIGNED', 'PREPARING']) {
    test('OrderDetailCubit does not start live tracking for $status', () async {
      final repo = _FakeCustomerOrderRepository(detailStatus: status);
      final cubit = OrderDetailCubit(repository: repo);

      await cubit.load('order-1');

      expect(cubit.state.status, OrderDetailStatus.success);
      expect(cubit.state.order?.status, status);
      expect(cubit.state.trackingPoints, hasLength(2));
      expect(cubit.state.isLiveTrackingEnabled, isFalse);
      expect(repo.detailLoads, 2);
      expect(repo.trackingLoads, 1);

      await cubit.close();
    });
  }

  test(
    'OrderDetailCubit appends realtime tracking points without polling',
    () async {
      final repo = _FakeCustomerOrderRepository();
      final realtime = _FakeOrderTrackingRealtimeService();
      final cubit = OrderDetailCubit(
        repository: repo,
        realtimeService: realtime,
      );

      await cubit.load('order-1');
      realtime.emit(const OrderTrackingRealtimeUpdate.connected());
      realtime.emit(
        OrderTrackingRealtimeUpdate.point(
          OrderTrackingPoint(
            lat: 10.764,
            lng: 106.663,
            recordedAt: DateTime.utc(2026, 1, 1, 10, 2),
          ),
        ),
      );
      await Future<void>.delayed(Duration.zero);

      expect(cubit.state.trackingPoints, hasLength(3));
      expect(cubit.state.trackingPoints.last.lat, 10.764);
      expect(repo.trackingLoads, 1);

      await cubit.close();
      expect(realtime.disposed, isTrue);
    },
  );

  testWidgets('Customer order detail shows tracking map for delivering order', (
    tester,
  ) async {
    await _pumpOrderDetailPage(tester, _FakeCustomerOrderRepository());

    expect(find.text('Live delivery tracking'), findsOneWidget);
    expect(find.text('Destination'), findsOneWidget);
    expect(find.text('Courier'), findsOneWidget);
  });

  testWidgets('Customer order detail hides tracking map before delivery', (
    tester,
  ) async {
    await _pumpOrderDetailPage(
      tester,
      _FakeCustomerOrderRepository(detailStatus: 'PREPARING'),
    );

    expect(find.text('Live delivery tracking'), findsNothing);
  });

  for (final status in ['PENDING', 'ACCEPTED', 'ASSIGNED']) {
    testWidgets('Customer order detail shows cancel button for $status', (
      tester,
    ) async {
      await _pumpOrderDetailPage(
        tester,
        _FakeCustomerOrderRepository(detailStatus: status),
      );

      expect(find.text('Cancel order'), findsOneWidget);
    });
  }

  for (final status in [
    'PREPARING',
    'DELIVERING',
    'SUCCESS',
    'FAILED',
    'CANCELLED',
  ]) {
    testWidgets('Customer order detail hides cancel button for $status', (
      tester,
    ) async {
      await _pumpOrderDetailPage(
        tester,
        _FakeCustomerOrderRepository(detailStatus: status),
      );

      expect(find.text('Cancel order'), findsNothing);
    });
  }
}

Future<void> _pumpOrderDetailPage(
  WidgetTester tester,
  _FakeCustomerOrderRepository orderRepository,
) async {
  await tester.pumpWidget(
    MultiRepositoryProvider(
      providers: [
        RepositoryProvider<CustomerOrderRepository>.value(
          value: orderRepository,
        ),
        RepositoryProvider<OrderTrackingRealtimeService>.value(
          value: _FakeOrderTrackingRealtimeService(),
        ),
      ],
      child: const MaterialApp(
        home: CustomerOrderDetailPage(orderId: 'order-1'),
      ),
    ),
  );
  await tester.pumpAndSettle();
}
