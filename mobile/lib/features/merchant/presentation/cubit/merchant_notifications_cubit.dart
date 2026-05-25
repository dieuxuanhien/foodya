import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/network/api_error_ui_message.dart';
import '../../domain/repositories/merchant_notification_repository.dart';
import 'merchant_notifications_state.dart';

class MerchantNotificationsCubit extends Cubit<MerchantNotificationsState> {
  MerchantNotificationsCubit({
    required MerchantNotificationRepository repository,
  }) : _repository = repository,
       super(const MerchantNotificationsState.initial());

  final MerchantNotificationRepository _repository;

  Future<void> load() async {
    emit(
      state.copyWith(
        status: MerchantNotificationsStatus.loading,
        clearError: true,
      ),
    );
    try {
      final notifications = await _repository.listNotifications();
      emit(
        state.copyWith(
          status:
              notifications.isEmpty
                  ? MerchantNotificationsStatus.empty
                  : MerchantNotificationsStatus.success,
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
          status: MerchantNotificationsStatus.failure,
          errorMessage: presentation.message,
        ),
      );
    }
  }

  void showUnreadOnly(bool value) {
    emit(state.copyWith(showUnreadOnly: value, clearError: true));
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
