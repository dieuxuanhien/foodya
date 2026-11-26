import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/network/api_error_ui_message.dart';
import '../../domain/repositories/delivery_repository.dart';
import 'delivery_wallet_state.dart';

class DeliveryWalletCubit extends Cubit<DeliveryWalletState> {
  DeliveryWalletCubit(this._repository) : super(const DeliveryWalletState());
  final DeliveryRepository _repository;

  Future<void> load() async {
    if (state.status == DeliveryWalletStatus.loading) return;
    emit(const DeliveryWalletState(status: DeliveryWalletStatus.loading));
    try {
      final report = await _repository.getEarnings();
      if (isClosed) return;
      emit(
        DeliveryWalletState(status: DeliveryWalletStatus.ready, report: report),
      );
    } catch (error) {
      if (isClosed) return;
      final message =
          ApiErrorUiMessageMapper.mapAny(
            error,
            fallback: 'Unable to load earnings.',
          ).message;
      emit(
        DeliveryWalletState(
          status: DeliveryWalletStatus.failure,
          errorMessage: message,
        ),
      );
    }
  }
}
