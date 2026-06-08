import '../models/customer_notification.dart';

abstract class CustomerNotificationRepository {
  Future<List<CustomerNotification>> listNotifications({
    int page = 0,
    int size = 20,
  });

  Future<CustomerNotification> markAsRead(String id);
}
