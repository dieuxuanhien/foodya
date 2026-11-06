import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:foodya_mobile/core/auth/session_cubit.dart';
import 'package:foodya_mobile/features/auth/data/repositories/mock_auth_repository.dart';
import 'package:foodya_mobile/features/auth/presentation/cubit/login_cubit.dart';
import 'package:foodya_mobile/features/delivery/data/repositories/demo_delivery_repository.dart';
import 'package:foodya_mobile/features/delivery/data/services/geolocator_delivery_position_provider.dart';
import 'package:foodya_mobile/features/delivery/domain/models/delivery_models.dart';
import 'package:foodya_mobile/features/delivery/domain/repositories/delivery_repository.dart';
import 'package:foodya_mobile/features/delivery/domain/services/delivery_position_provider.dart';
import 'package:foodya_mobile/features/delivery/presentation/cubit/delivery_home_cubit.dart';
import 'package:foodya_mobile/features/delivery/presentation/cubit/delivery_home_state.dart';
import 'package:foodya_mobile/features/delivery/presentation/cubit/delivery_assignments_cubit.dart';
import 'package:foodya_mobile/features/delivery/presentation/cubit/delivery_assignments_state.dart';
import 'package:foodya_mobile/features/delivery/presentation/cubit/delivery_order_cubit.dart';
import 'package:foodya_mobile/features/delivery/presentation/pages/delivery_batch_detail_page.dart';
import 'package:foodya_mobile/features/delivery/presentation/pages/delivery_batch_management_page.dart';
import 'package:foodya_mobile/features/delivery/presentation/pages/delivery_assignments_page.dart';
import 'package:foodya_mobile/features/delivery/presentation/pages/delivery_order_page.dart';
import 'package:foodya_mobile/features/delivery/presentation/pages/delivery_home_page.dart';
import 'package:foodya_mobile/features/delivery/presentation/pages/delivery_wallet_page.dart';

void main() {
  test(
    'driver session parses the verified backend response and unknown status',
    () {
      final session = DeliverySession.fromJson({
        'sessionId': '14141414-1414-1414-1414-141414141414',
        'status': 'ONLINE',
        'startedAt': '2026-11-05T08:00:00Z',
        'currentLat': 10.7769,
        'currentLng': 106.7009,
        'activeOrderCount': 1,
        'maxConcurrentOrders': 3,
      });

      expect(session.status, DriverAvailability.online);
      expect(session.startedAt, DateTime.utc(2026, 11, 5, 8));
      expect(session.maxConcurrentOrders, 3);
      expect(
        DriverAvailability.fromApi('FUTURE_STATUS'),
        DriverAvailability.unknown,
      );
    },
  );

  test('assignment parser tolerates summary fields missing from backend', () {
    final assignment = DeliveryAssignment.fromJson({
      'orderId': 'order-1',
      'orderCode': 'FY-001',
      'customerName': 'Customer',
      'restaurantName': 'Restaurant',
      'status': 'ASSIGNED',
      'paymentStatus': 'UNPAID',
      'totalAmount': 125000,
    });

    expect(assignment.deliveryAddress, isNull);
    expect(assignment.totalAmount, 125000);
    expect(assignment.status, DeliveryOrderStatus.assigned);
  });

  test('batch, route and earnings parse verified DTO shapes defensively', () {
    final batch = DeliveryBatch.fromJson({
      'batchId': 'batch-1',
      'status': 'ACTIVE',
      'pickupSequence': '[{"orderId":"order-1"}]',
      'deliverySequence': 'invalid-json',
      'totalDistanceMeters': 8000,
      'items': [
        {
          'id': 'item-1',
          'orderId': 'order-1',
          'pickupIndex': 1,
          'deliveryIndex': 1,
          'itemStatus': 'PENDING',
        },
      ],
    });
    final route = DeliveryRouteSummary.fromJson({
      'distanceMeters': 8000,
      'durationSeconds': 1200,
      'waypoints': [
        {'lat': 10.77, 'lng': 106.7, 'type': 'PICKUP'},
      ],
    });
    final earnings = DeliveryEarningsReport.fromJson({
      'from': '2026-10-01T00:00:00Z',
      'to': '2026-11-01T00:00:00Z',
      'totalEarnings': 27000,
      'codCashCollected': 180000,
      'deliveryCount': 1,
      'entries': [
        {
          'orderId': 'order-1',
          'orderCode': 'FY-001',
          'completedAt': '2026-10-05T03:00:00Z',
          'deliveryFee': 30000,
          'platformMargin': 3000,
          'driverEarning': 27000,
          'paymentMethod': 'COD',
        },
      ],
    });

    expect(batch.pickupSequence.single.orderId, 'order-1');
    expect(batch.deliverySequence, isEmpty);
    expect(batch.stops.single.restaurantName, 'Restaurant unavailable');
    expect(route.waypoints.single.type, 'PICKUP');
    expect(earnings.entries.single.driverEarning, 27000);
  });

  test(
    'all verified Delivery statuses parse and zero sequence coordinates hide',
    () {
      expect(
        DeliveryOrderStatus.fromApi('ACCEPTED'),
        DeliveryOrderStatus.accepted,
      );
      expect(
        DeliveryOrderStatus.fromApi('CANCELLED'),
        DeliveryOrderStatus.cancelled,
      );
      expect(
        DeliveryBatchStatus.fromApi('PENDING'),
        DeliveryBatchStatus.pending,
      );
      expect(
        DeliveryBatchStatus.fromApi('PARTIAL'),
        DeliveryBatchStatus.partial,
      );
      expect(
        DeliveryStopStatus.fromApi('DELIVERING'),
        DeliveryStopStatus.delivering,
      );
      expect(
        DeliveryBatchStatus.fromApi('FUTURE'),
        DeliveryBatchStatus.unknown,
      );

      final batch = DeliveryBatch.fromJson({
        'batchId': 'batch-zero',
        'status': 'PENDING',
        'pickupSequence':
            '[{"orderId":"order-1","lat":0,"lng":0,"estimatedArrivalSec":30,"serviceTimeSec":10}]',
        'items': <Object?>[],
      });
      expect(batch.pickupSequence.single.hasUsableCoordinates, isFalse);
      expect(batch.pickupSequence.single.estimatedArrivalSeconds, 30);
    },
  );

  test(
    'all deterministic demo scenarios can be loaded or fail intentionally',
    () async {
      for (final scenario in DeliveryDemoScenario.values) {
        final repository = DemoDeliveryRepository(scenario: scenario);
        if (scenario == DeliveryDemoScenario.networkError) {
          await expectLater(
            repository.getSession(),
            throwsA(isA<DeliveryFeatureUnavailableException>()),
          );
        } else {
          expect(await repository.getSession(), isA<DeliverySession>());
          expect(
            await repository.getAssignments(),
            isA<List<DeliveryAssignment>>(),
          );
        }
      }
    },
  );

  test('home cubit ignores delayed completion after disposal', () async {
    final cubit = DeliveryHomeCubit(
      DemoDeliveryRepository(),
      const DemoDeliveryPositionProvider(),
    );
    final load = cubit.load();
    await cubit.close();
    await expectLater(load, completes);
  });

  test('home cubit prevents duplicate availability actions', () async {
    final cubit = DeliveryHomeCubit(
      DemoDeliveryRepository(scenario: DeliveryDemoScenario.offline),
      const DemoDeliveryPositionProvider(),
    );
    await cubit.load();

    final first = cubit.toggleAvailability();
    final second = cubit.toggleAvailability();
    await Future.wait([first, second]);

    expect(cubit.state.status, DeliveryHomeStatus.ready);
    expect(cubit.state.session?.status, DriverAvailability.online);
    await cubit.close();
  });

  test(
    'home goes online without coordinates when permission is denied',
    () async {
      final cubit = DeliveryHomeCubit(
        DemoDeliveryRepository(scenario: DeliveryDemoScenario.offline),
        const _DeniedPositionProvider(),
      );
      await cubit.load();
      await cubit.toggleAvailability();
      expect(cubit.state.session?.status, DriverAvailability.online);
      expect(cubit.state.infoMessage, contains('matching requires a location'));
      await cubit.close();
    },
  );

  test(
    'assignments cubit blocks duplicate acknowledgment and refreshes',
    () async {
      final repository = _CountingDemoDeliveryRepository();
      final cubit = DeliveryAssignmentsCubit(repository);
      await cubit.load();
      await Future.wait([
        cubit.acknowledge('demo-order-1'),
        cubit.acknowledge('demo-order-1'),
      ]);
      expect(repository.acceptCalls, 1);
      expect(cubit.state.status, DeliveryAssignmentsStatus.ready);
      expect(cubit.state.infoMessage, contains('acknowledged'));
      await cubit.close();
    },
  );

  test('order cubit enriches detail and executes valid transitions', () async {
    final repository = DemoDeliveryRepository();
    final cubit = DeliveryOrderCubit(repository);
    await cubit.load('demo-order-1');
    expect(cubit.state.route?.distanceMeters, 8200);
    await cubit.acknowledge();
    expect(cubit.state.order?.deliveryAddress, isNotNull);
    await cubit.updateStatus(DeliveryOrderStatus.delivering);
    expect(cubit.state.order?.status, DeliveryOrderStatus.delivering);
    await cubit.updateStatus(DeliveryOrderStatus.success);
    expect(cubit.state.order?.status, DeliveryOrderStatus.success);
    await cubit.close();
  });

  testWidgets(
    'delivery home renders session controls without narrow overflow',
    (tester) async {
      tester.view.physicalSize = const Size(390, 844);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      await _pumpDeliveryPage(
        tester,
        repository: DemoDeliveryRepository(),
        child: const DeliveryHomePage(),
      );
      await tester.pumpAndSettle();

      expect(find.textContaining('Demo Data'), findsOneWidget);
      expect(find.text('Driver actions'), findsOneWidget);
      expect(find.text('View assigned orders'), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('batch detail and management render progress states', (
    tester,
  ) async {
    final repository = DemoDeliveryRepository(
      scenario: DeliveryDemoScenario.batchProgress,
    );
    await _pumpDeliveryPage(
      tester,
      repository: repository,
      child: const DeliveryBatchDetailPage(batchId: 'active'),
    );
    await tester.pumpAndSettle();
    expect(find.text('Route visualization placeholder'), findsOneWidget);
    expect(find.text('BATCH-DEMO-2404'), findsOneWidget);

    await _pumpDeliveryPage(
      tester,
      repository: repository,
      child: const DeliveryBatchManagementPage(batchId: 'active'),
    );
    await tester.pumpAndSettle();
    expect(find.text('Pickup'), findsOneWidget);
    expect(find.text('Delivery'), findsOneWidget);
    expect(find.text('Payment'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('assignments and order detail render verified actions', (
    tester,
  ) async {
    final repository = DemoDeliveryRepository();
    await _pumpDeliveryPage(
      tester,
      repository: repository,
      child: const DeliveryAssignmentsPage(),
    );
    await tester.pumpAndSettle();
    expect(find.text('FY-241105-001'), findsOneWidget);
    expect(find.text('Acknowledge'), findsOneWidget);
    expect(find.text('Release'), findsOneWidget);

    await _pumpDeliveryPage(
      tester,
      repository: repository,
      child: const DeliveryOrderPage(orderId: 'demo-order-1'),
    );
    await tester.pumpAndSettle();
    expect(find.text('Planned route summary'), findsOneWidget);
    expect(find.text('Start delivery'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('earnings report labels fixture values as read only', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(1280, 800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    await _pumpDeliveryPage(
      tester,
      repository: DemoDeliveryRepository(
        scenario: DeliveryDemoScenario.walletPreview,
      ),
      child: const DeliveryWalletPage(),
    );
    await tester.pumpAndSettle();

    expect(find.text('Delivery earnings · VND'), findsOneWidget);
    expect(find.text('Read-only report'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('network error renders an explicit retry state', (tester) async {
    await _pumpDeliveryPage(
      tester,
      repository: DemoDeliveryRepository(
        scenario: DeliveryDemoScenario.networkError,
      ),
      child: const DeliveryHomePage(),
    );
    await tester.pumpAndSettle();
    expect(find.text('Retry'), findsOneWidget);
    expect(find.textContaining('Unable to load Delivery status'), findsWidgets);
  });
}

class _DeniedPositionProvider implements DeliveryPositionProvider {
  const _DeniedPositionProvider();

  @override
  Future<DeliveryPositionResult> getCurrentPosition() async =>
      const DeliveryPositionResult.failure(
        DeliveryPositionFailure.permissionDenied,
      );
}

class _CountingDemoDeliveryRepository extends DemoDeliveryRepository {
  int acceptCalls = 0;

  @override
  Future<DeliveryAssignment> acceptAssignment(String orderId) {
    acceptCalls++;
    return super.acceptAssignment(orderId);
  }
}

Future<void> _pumpDeliveryPage(
  WidgetTester tester, {
  required DeliveryRepository repository,
  required Widget child,
}) async {
  final sessionCubit = SessionCubit();
  final authRepository = MockAuthRepository();
  addTearDown(sessionCubit.close);
  await tester.pumpWidget(
    MultiRepositoryProvider(
      providers: [
        RepositoryProvider<DeliveryRepository>.value(value: repository),
        RepositoryProvider<DeliveryPositionProvider>.value(
          value: const DemoDeliveryPositionProvider(),
        ),
      ],
      child: MultiBlocProvider(
        providers: [
          BlocProvider<SessionCubit>.value(value: sessionCubit),
          BlocProvider<LoginCubit>(
            create:
                (_) => LoginCubit(
                  authRepository: authRepository,
                  sessionCubit: sessionCubit,
                ),
          ),
        ],
        child: MaterialApp(home: child),
      ),
    ),
  );
}
