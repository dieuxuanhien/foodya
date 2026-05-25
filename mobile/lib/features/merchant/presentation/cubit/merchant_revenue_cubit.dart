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
    return load(from: from, to: to, topItems: state.topItems);
  }

  Future<void> loadLast7Days() {
    final to = DateTime.now();
    final from = to.subtract(const Duration(days: 7));
    return load(from: from, to: to, topItems: state.topItems);
  }

  Future<void> loadLast90Days() {
    final to = DateTime.now();
    final from = to.subtract(const Duration(days: 90));
    return load(from: from, to: to, topItems: state.topItems);
  }

  Future<void> setTopItems(int value) {
    final normalized = value.clamp(3, 20);
    return load(from: state.from, to: state.to, topItems: normalized);
  }

  Future<void> setDateRange({required DateTime from, required DateTime to}) {
    return load(from: from, to: to, topItems: state.topItems);
  }

  Future<void> load({DateTime? from, DateTime? to, int topItems = 5}) async {
    if (state.isLoading) {
      return;
    }
    emit(
      state.copyWith(
        status: MerchantRevenueStatus.loading,
        from: from,
        to: to,
        topItems: topItems,
        clearError: true,
      ),
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
