import 'package:equatable/equatable.dart';

import '../../domain/models/delivery_models.dart';

enum DeliveryWalletStatus { initial, loading, ready, failure }

class DeliveryWalletState extends Equatable {
  const DeliveryWalletState({
    this.status = DeliveryWalletStatus.initial,
    this.report,
    this.errorMessage,
  });
  final DeliveryWalletStatus status;
  final DeliveryEarningsReport? report;
  final String? errorMessage;
  @override
  List<Object?> get props => [status, report, errorMessage];
}
