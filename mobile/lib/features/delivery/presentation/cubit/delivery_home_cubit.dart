import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/network/api_error_ui_message.dart';
import '../../domain/models/delivery_models.dart';
import '../../domain/repositories/delivery_repository.dart';
import '../../domain/services/delivery_position_provider.dart';
import 'delivery_home_state.dart';

class DeliveryHomeCubit extends Cubit<DeliveryHomeState> {
  DeliveryHomeCubit(this._repository, this._positionProvider)
    : super(const DeliveryHomeState());

  final DeliveryRepository _repository;
  final DeliveryPositionProvider _positionProvider;

  Future<void> load() async {
    if (state.isBusy) return;
    emit(
      state.copyWith(
        status: DeliveryHomeStatus.loading,
        clearError: true,
        clearInfo: true,
      ),
    );
    try {
      final session = await _repository.getSession();
      if (isClosed) return;
      emit(
        state.copyWith(
          status: DeliveryHomeStatus.ready,
          session: session,
          clearError: true,
        ),
      );
    } catch (error) {
      _emitFailure(error, 'Unable to load Delivery status.');
    }
  }

  Future<void> toggleAvailability() async {
    final session = state.session;
    if (session == null || state.isBusy || !session.canToggleAvailability) {
      return;
    }
    emit(
      state.copyWith(
        status: DeliveryHomeStatus.actionPending,
        clearError: true,
        clearInfo: true,
      ),
    );
    try {
      DeliveryPositionResult? position;
      late final DeliverySession updated;
      if (session.isOnline) {
        updated = await _repository.goOffline();
      } else {
        position = await _positionProvider.getCurrentPosition();
        updated = await _repository.goOnline(
          lat: position.lat,
          lng: position.lng,
        );
      }
      if (isClosed) return;
      emit(
        state.copyWith(
          status: DeliveryHomeStatus.ready,
          session: updated,
          infoMessage:
              position != null && !position.hasPosition
                  ? 'You are online, but matching requires a location update.'
                  : updated.message ?? 'Availability updated.',
        ),
      );
    } catch (error) {
      _emitFailure(error, 'Unable to update availability.');
    }
  }

  Future<void> refreshLocation() async {
    final session = state.session;
    if (session == null || !session.isOnline || state.isBusy) return;
    emit(
      state.copyWith(
        status: DeliveryHomeStatus.actionPending,
        clearError: true,
        clearInfo: true,
      ),
    );
    try {
      final position = await _positionProvider.getCurrentPosition();
      if (!position.hasPosition) {
        if (isClosed) return;
        emit(
          state.copyWith(
            status: DeliveryHomeStatus.ready,
            errorMessage: _positionFailureMessage(position.failure),
          ),
        );
        return;
      }
      final updated = await _repository.updateLocation(
        position.lat!,
        position.lng!,
      );
      if (isClosed) return;
      emit(
        state.copyWith(
          status: DeliveryHomeStatus.ready,
          session: updated,
          infoMessage: 'Current location updated.',
        ),
      );
    } catch (error) {
      _emitFailure(error, 'Unable to update your location.');
    }
  }

  void clearFeedback() {
    if (!isClosed) emit(state.copyWith(clearError: true, clearInfo: true));
  }

  void _emitFailure(Object error, String fallback) {
    if (isClosed) return;
    final message =
        ApiErrorUiMessageMapper.mapAny(error, fallback: fallback).message;
    emit(
      state.copyWith(
        status:
            state.session == null
                ? DeliveryHomeStatus.failure
                : DeliveryHomeStatus.ready,
        errorMessage: message,
      ),
    );
  }

  String _positionFailureMessage(DeliveryPositionFailure? failure) =>
      switch (failure) {
        DeliveryPositionFailure.serviceDisabled =>
          'Turn on location services before updating your position.',
        DeliveryPositionFailure.permissionDenied =>
          'Location permission was denied. Matching requires a location.',
        DeliveryPositionFailure.permissionPermanentlyDenied =>
          'Location permission is blocked. Enable it in system settings.',
        _ => 'Current location is unavailable. Please try again.',
      };
}
