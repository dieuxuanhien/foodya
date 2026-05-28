import 'package:flutter_test/flutter_test.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_notification.dart';
import 'package:foodya_mobile/features/merchant/domain/repositories/merchant_notification_repository.dart';
import 'package:foodya_mobile/features/merchant/presentation/cubit/merchant_notifications_cubit.dart';
import 'package:foodya_mobile/features/merchant/presentation/cubit/merchant_notifications_state.dart';

class _FakeMerchantNotificationRepository
    implements MerchantNotificationRepository {
  _FakeMerchantNotificationRepository({this.shouldFail = false});

  final bool shouldFail;

  List<MerchantNotification> notifications = [
    MerchantNotification(
      id: 'notification-1',
      receiverUserId: 'merchant-1',
      receiverType: 'MERCHANT',
      eventType: 'ORDER_CREATED',
      title: 'New order',
      message: 'Order FDY-001 is waiting for confirmation.',
      status: 'SENT',
      orderId: 'order-1',
      sentAt: DateTime(2026, 1, 1, 10),
      createdAt: DateTime(2026, 1, 1, 10),
    ),
  ];

  @override
  Future<List<MerchantNotification>> listNotifications({
    int page = 0,
    int size = 20,
  }) async {
    if (shouldFail) {
      throw Exception('notifications');
    }
    return notifications;
  }

  @override
  Future<MerchantNotification> markAsRead(String id) async {
    final current = notifications.singleWhere(
      (notification) => notification.id == id,
    );
    final updated = MerchantNotification(
      id: current.id,
      receiverUserId: current.receiverUserId,
      receiverType: current.receiverType,
      eventType: current.eventType,
      title: current.title,
      message: current.message,
      status: 'READ',
      orderId: current.orderId,
      sentAt: current.sentAt,
      readAt: DateTime(2026, 1, 1, 10, 5),
      createdAt: current.createdAt,
    );
    notifications = [updated];
    return updated;
  }
}

void main() {
  test('MerchantNotificationsCubit loads unread notifications', () async {
    final cubit = MerchantNotificationsCubit(
      repository: _FakeMerchantNotificationRepository(),
    );

    await cubit.load();

    expect(cubit.state.status, MerchantNotificationsStatus.success);
    expect(cubit.state.unreadCount, 1);
    expect(cubit.state.notifications.single.title, 'New order');
  });

  test('MerchantNotificationsCubit filters unread and marks as read', () async {
    final cubit = MerchantNotificationsCubit(
      repository: _FakeMerchantNotificationRepository(),
    );

    await cubit.load();
    cubit.showUnreadOnly(true);

    expect(cubit.state.visibleNotifications, hasLength(1));

    await cubit.markAsRead('notification-1');

    expect(cubit.state.unreadCount, 0);
    expect(cubit.state.visibleNotifications, isEmpty);
    expect(cubit.state.notifications.single.isRead, isTrue);
  });

  test('MerchantNotificationsCubit handles empty and failure states', () async {
    final emptyRepository =
        _FakeMerchantNotificationRepository()..notifications = [];
    final emptyCubit = MerchantNotificationsCubit(repository: emptyRepository);

    await emptyCubit.load();

    expect(emptyCubit.state.status, MerchantNotificationsStatus.empty);

    final failureCubit = MerchantNotificationsCubit(
      repository: _FakeMerchantNotificationRepository(shouldFail: true),
    );

    await failureCubit.load();

    expect(failureCubit.state.status, MerchantNotificationsStatus.failure);
    expect(failureCubit.state.errorMessage, isNotEmpty);
  });
}
