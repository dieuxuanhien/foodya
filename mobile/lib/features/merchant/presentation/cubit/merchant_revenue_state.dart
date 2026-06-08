import 'package:equatable/equatable.dart';

import '../../domain/models/merchant_revenue_report.dart';

enum MerchantRevenueStatus { initial, loading, success, failure }

class MerchantRevenueState extends Equatable {
  const MerchantRevenueState({
    required this.status,
    required this.from,
    required this.to,
    required this.topItems,
    this.report,
    this.errorMessage,
  });

  const MerchantRevenueState.initial()
    : this(
        status: MerchantRevenueStatus.initial,
        from: null,
        to: null,
        topItems: 5,
      );

  final MerchantRevenueStatus status;
  final DateTime? from;
  final DateTime? to;
  final int topItems;
  final MerchantRevenueReport? report;
  final String? errorMessage;

  bool get isLoading => status == MerchantRevenueStatus.loading;

  MerchantRevenueState copyWith({
    MerchantRevenueStatus? status,
    DateTime? from,
    DateTime? to,
    int? topItems,
    MerchantRevenueReport? report,
    String? errorMessage,
    bool clearError = false,
  }) {
    return MerchantRevenueState(
      status: status ?? this.status,
      from: from ?? this.from,
      to: to ?? this.to,
      topItems: topItems ?? this.topItems,
      report: report ?? this.report,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
    );
  }

  @override
  List<Object?> get props => [status, from, to, topItems, report, errorMessage];
}
