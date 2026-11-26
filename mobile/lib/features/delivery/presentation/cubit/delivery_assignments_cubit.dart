import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/network/api_error_ui_message.dart';
import '../../domain/repositories/delivery_repository.dart';
import 'delivery_assignments_state.dart';

class DeliveryAssignmentsCubit extends Cubit<DeliveryAssignmentsState> {
  DeliveryAssignmentsCubit(this._repository)
    : super(const DeliveryAssignmentsState());

  final DeliveryRepository _repository;

  Future<void> load({bool showLoading = true}) async {
    if (state.pendingOrderId != null) return;
    if (showLoading) {
      emit(
        state.copyWith(
          status: DeliveryAssignmentsStatus.loading,
          clearError: true,
          clearInfo: true,
        ),
      );
    }
    try {
      final assignments = await _repository.getAssignments();
      if (isClosed) return;
      emit(
        state.copyWith(
          status:
              assignments.isEmpty
                  ? DeliveryAssignmentsStatus.empty
                  : DeliveryAssignmentsStatus.ready,
          assignments: assignments,
          clearError: true,
        ),
      );
    } catch (error) {
      if (isClosed) return;
      final message =
          ApiErrorUiMessageMapper.mapAny(
            error,
            fallback: 'Unable to load assigned orders.',
          ).message;
      emit(
        state.copyWith(
          status:
              state.assignments.isEmpty
                  ? DeliveryAssignmentsStatus.failure
                  : DeliveryAssignmentsStatus.ready,
          errorMessage: message,
        ),
      );
    }
  }

  Future<void> acknowledge(String orderId) => _respond(orderId, true);
  Future<void> reject(String orderId) => _respond(orderId, false);

  Future<void> _respond(String orderId, bool acknowledge) async {
    if (state.pendingOrderId != null) return;
    emit(
      state.copyWith(
        pendingOrderId: orderId,
        clearError: true,
        clearInfo: true,
      ),
    );
    try {
      if (acknowledge) {
        await _repository.acceptAssignment(orderId);
      } else {
        await _repository.rejectAssignment(orderId);
      }
      final assignments = await _repository.getAssignments();
      if (isClosed) return;
      emit(
        state.copyWith(
          status:
              assignments.isEmpty
                  ? DeliveryAssignmentsStatus.empty
                  : DeliveryAssignmentsStatus.ready,
          assignments: assignments,
          clearPending: true,
          infoMessage:
              acknowledge
                  ? 'Assignment acknowledged. The backend does not persist a separate acceptance flag.'
                  : 'Assignment released for reassignment.',
        ),
      );
    } catch (error) {
      if (isClosed) return;
      final message =
          ApiErrorUiMessageMapper.mapAny(
            error,
            fallback:
                acknowledge
                    ? 'Unable to acknowledge this assignment.'
                    : 'Unable to release this assignment.',
          ).message;
      emit(state.copyWith(clearPending: true, errorMessage: message));
      await load(showLoading: false);
    }
  }

  void clearFeedback() {
    if (!isClosed) emit(state.copyWith(clearError: true, clearInfo: true));
  }
}
