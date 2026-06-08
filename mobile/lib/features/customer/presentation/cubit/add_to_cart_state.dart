import 'package:equatable/equatable.dart';

enum AddToCartStatus { idle, adding, success, failure }

class AddToCartState extends Equatable {
  const AddToCartState({
    required this.status,
    this.errorMessage,
    this.infoMessage,
  });

  const AddToCartState.initial() : this(status: AddToCartStatus.idle);

  final AddToCartStatus status;
  final String? errorMessage;
  final String? infoMessage;

  bool get isBusy => status == AddToCartStatus.adding;

  AddToCartState copyWith({
    AddToCartStatus? status,
    String? errorMessage,
    String? infoMessage,
    bool clearError = false,
    bool clearInfo = false,
  }) {
    return AddToCartState(
      status: status ?? this.status,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      infoMessage: clearInfo ? null : (infoMessage ?? this.infoMessage),
    );
  }

  @override
  List<Object?> get props => [status, errorMessage, infoMessage];
}
