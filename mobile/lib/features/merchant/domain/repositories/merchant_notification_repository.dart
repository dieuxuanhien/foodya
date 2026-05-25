import '../models/merchant_notification.dart';

abstract class MerchantNotificationRepository {
  Future<List<MerchantNotification>> listNotifications({
    int page = 0,
    int size = 20,
  });

  Future<MerchantNotification> markAsRead(String id);
}
