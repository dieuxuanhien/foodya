import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/network/api_error_ui_message.dart';
import '../../domain/repositories/merchant_revenue_repository.dart';
import 'merchant_revenue_state.dart';

class MerchantRevenueCubit extends Cubit<MerchantRevenueState> {
  MerchantRevenueCubit({required MerchantRevenueRepository repository})
    : _repository = repository,
      super(const MerchantRevenueState.initial());

  final MerchantRevenueRepository _repository;

  Future<void> loadLast30Days() {
    final to = DateTime.now();
    final from = to.subtract(const Duration(days: 30));
    return load(from: from, to: to, topItems: 5);
  }

  Future<void> load({DateTime? from, DateTime? to, int topItems = 5}) async {
    if (state.isLoading) {
      return;
    }
    emit(
      state.copyWith(status: MerchantRevenueStatus.loading, clearError: true),
    );
    try {
      final report = await _repository.getRevenueReport(
        from: from,
        to: to,
        topItems: topItems,
      );
      emit(
        state.copyWith(
          status: MerchantRevenueStatus.success,
          report: report,
          clearError: true,
        ),
      );
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to load revenue report.',
      );
      emit(
        state.copyWith(
          status: MerchantRevenueStatus.failure,
          errorMessage: presentation.message,
        ),
      );
    }
  }
}
