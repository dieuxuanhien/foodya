import 'package:flutter_bloc/flutter_bloc.dart';

import '../../domain/repositories/delivery_repository.dart';
import 'delivery_home_state.dart';

class DeliveryHomeCubit extends Cubit<DeliveryHomeState> {
  DeliveryHomeCubit(this._repository) : super(const DeliveryHomeState());
  final DeliveryRepository _repository;

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
      final assignments = await _repository.getAssignments();
      final batch = await _repository.getActiveBatch();
      if (isClosed) return;
      emit(
        state.copyWith(
          status:
              assignments.isEmpty && batch == null
                  ? DeliveryHomeStatus.empty
                  : DeliveryHomeStatus.ready,
          session: session,
          assignments: assignments,
          activeBatch: batch,
          clearBatch: batch == null,
          clearError: true,
        ),
      );
    } catch (error) {
      if (isClosed) return;
      emit(
        state.copyWith(
          status: DeliveryHomeStatus.failure,
          errorMessage: error.toString(),
        ),
      );
    }
  }

  Future<void> toggleAvailability() async {
    final session = state.session;
    if (session == null || state.isBusy) return;
    emit(
      state.copyWith(
        status: DeliveryHomeStatus.actionPending,
        clearError: true,
        clearInfo: true,
      ),
    );
    try {
      final updated =
          session.isOnline
              ? await _repository.goOffline()
              : await _repository.goOnline();
      if (isClosed) return;
      emit(
        state.copyWith(
          status: DeliveryHomeStatus.ready,
          session: updated,
          infoMessage: updated.message ?? 'Availability updated.',
        ),
      );
    } catch (error) {
      if (isClosed) return;
      emit(
        state.copyWith(
          status: DeliveryHomeStatus.failure,
          errorMessage: error.toString(),
        ),
      );
    }
  }

  Future<void> accept(String orderId) => _assignmentAction(orderId, true);
  Future<void> reject(String orderId) => _assignmentAction(orderId, false);

  Future<void> _assignmentAction(String orderId, bool accept) async {
    if (state.isBusy) return;
    emit(
      state.copyWith(
        status: DeliveryHomeStatus.actionPending,
        clearError: true,
        clearInfo: true,
      ),
    );
    try {
      if (accept) {
        await _repository.acceptAssignment(orderId);
      } else {
        await _repository.rejectAssignment(orderId);
      }
      final assignments = await _repository.getAssignments();
      if (isClosed) return;
      emit(
        state.copyWith(
          status:
              assignments.isEmpty && state.activeBatch == null
                  ? DeliveryHomeStatus.empty
                  : DeliveryHomeStatus.ready,
          assignments: assignments,
          infoMessage:
              accept
                  ? 'Assignment acknowledged in demo mode.'
                  : 'Assignment released in demo mode.',
        ),
      );
    } catch (error) {
      if (isClosed) return;
      emit(
        state.copyWith(
          status: DeliveryHomeStatus.failure,
          errorMessage: error.toString(),
        ),
      );
    }
  }

  void clearFeedback() {
    if (!isClosed) emit(state.copyWith(clearError: true, clearInfo: true));
  }
}
