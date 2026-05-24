import 'package:equatable/equatable.dart';

import '../../domain/models/merchant_revenue_report.dart';

enum MerchantRevenueStatus { initial, loading, success, failure }

class MerchantRevenueState extends Equatable {
  const MerchantRevenueState({
    required this.status,
    this.report,
    this.errorMessage,
  });

  const MerchantRevenueState.initial()
    : this(status: MerchantRevenueStatus.initial);

  final MerchantRevenueStatus status;
  final MerchantRevenueReport? report;
  final String? errorMessage;

  bool get isLoading => status == MerchantRevenueStatus.loading;

  MerchantRevenueState copyWith({
    MerchantRevenueStatus? status,
    MerchantRevenueReport? report,
    String? errorMessage,
    bool clearError = false,
  }) {
    return MerchantRevenueState(
      status: status ?? this.status,
      report: report ?? this.report,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
    );
  }

  @override
  List<Object?> get props => [status, report, errorMessage];
}
