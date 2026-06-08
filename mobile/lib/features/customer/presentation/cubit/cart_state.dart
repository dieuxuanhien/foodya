import 'package:equatable/equatable.dart';

import '../../domain/models/active_cart.dart';

enum CartStatus { initial, loading, success, empty, updating, failure }

class CartState extends Equatable {
  const CartState({
    required this.status,
    required this.cart,
    this.errorMessage,
    this.infoMessage,
  });

  const CartState.initial() : this(status: CartStatus.initial, cart: null);

  final CartStatus status;
  final ActiveCart? cart;
  final String? errorMessage;
  final String? infoMessage;

  bool get isBusy =>
      status == CartStatus.loading || status == CartStatus.updating;

  bool get hasItems => cart != null && cart!.items.isNotEmpty;

  CartState copyWith({
    CartStatus? status,
    ActiveCart? cart,
    bool clearCart = false,
    String? errorMessage,
    String? infoMessage,
    bool clearError = false,
    bool clearInfo = false,
  }) {
    return CartState(
      status: status ?? this.status,
      cart: clearCart ? null : (cart ?? this.cart),
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      infoMessage: clearInfo ? null : (infoMessage ?? this.infoMessage),
    );
  }

  @override
  List<Object?> get props => [status, cart, errorMessage, infoMessage];
}
