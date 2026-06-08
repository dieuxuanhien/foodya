import 'package:equatable/equatable.dart';

import '../../domain/models/customer_notification.dart';

enum NotificationStatus { initial, loading, success, empty, failure }

class NotificationState extends Equatable {
  const NotificationState({
    required this.status,
    required this.notifications,
    this.errorMessage,
  });

  const NotificationState.initial()
    : this(status: NotificationStatus.initial, notifications: const []);

  final NotificationStatus status;
  final List<CustomerNotification> notifications;
  final String? errorMessage;

  NotificationState copyWith({
    NotificationStatus? status,
    List<CustomerNotification>? notifications,
    String? errorMessage,
    bool clearError = false,
  }) {
    return NotificationState(
      status: status ?? this.status,
      notifications: notifications ?? this.notifications,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
    );
  }

  @override
  List<Object?> get props => [status, notifications, errorMessage];
}
