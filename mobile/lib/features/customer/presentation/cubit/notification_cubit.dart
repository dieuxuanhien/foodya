import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/network/api_error_ui_message.dart';
import '../../domain/repositories/customer_notification_repository.dart';
import 'notification_state.dart';

class NotificationCubit extends Cubit<NotificationState> {
  NotificationCubit({required CustomerNotificationRepository repository})
    : _repository = repository,
      super(const NotificationState.initial());

  final CustomerNotificationRepository _repository;

  Future<void> load() async {
    emit(state.copyWith(status: NotificationStatus.loading, clearError: true));
    try {
      final notifications = await _repository.listNotifications();
      emit(
        state.copyWith(
          status:
              notifications.isEmpty
                  ? NotificationStatus.empty
                  : NotificationStatus.success,
          notifications: notifications,
          clearError: true,
        ),
      );
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to load notifications.',
      );
      emit(
        state.copyWith(
          status: NotificationStatus.failure,
          errorMessage: presentation.message,
        ),
      );
    }
  }

  Future<void> markAsRead(String id) async {
    try {
      final updated = await _repository.markAsRead(id);
      final next = state.notifications
          .map((item) => item.id == id ? updated : item)
          .toList(growable: false);
      emit(state.copyWith(notifications: next, clearError: true));
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to mark notification as read.',
      );
      emit(state.copyWith(errorMessage: presentation.message));
    }
  }
}
