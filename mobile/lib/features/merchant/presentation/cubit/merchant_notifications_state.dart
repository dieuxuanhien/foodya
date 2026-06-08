import 'package:equatable/equatable.dart';

import '../../domain/models/merchant_notification.dart';

enum MerchantNotificationsStatus { initial, loading, success, empty, failure }

class MerchantNotificationsState extends Equatable {
  const MerchantNotificationsState({
    required this.status,
    required this.notifications,
    required this.showUnreadOnly,
    this.errorMessage,
  });

  const MerchantNotificationsState.initial()
    : this(
        status: MerchantNotificationsStatus.initial,
        notifications: const [],
        showUnreadOnly: false,
      );

  final MerchantNotificationsStatus status;
  final List<MerchantNotification> notifications;
  final bool showUnreadOnly;
  final String? errorMessage;

  int get unreadCount =>
      notifications.where((notification) => !notification.isRead).length;

  List<MerchantNotification> get visibleNotifications {
    if (!showUnreadOnly) {
      return notifications;
    }
    return notifications
        .where((notification) => !notification.isRead)
        .toList(growable: false);
  }

  MerchantNotificationsState copyWith({
    MerchantNotificationsStatus? status,
    List<MerchantNotification>? notifications,
    bool? showUnreadOnly,
    String? errorMessage,
    bool clearError = false,
  }) {
    return MerchantNotificationsState(
      status: status ?? this.status,
      notifications: notifications ?? this.notifications,
      showUnreadOnly: showUnreadOnly ?? this.showUnreadOnly,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
    );
  }

  @override
  List<Object?> get props => [
    status,
    notifications,
    showUnreadOnly,
    errorMessage,
  ];
}
