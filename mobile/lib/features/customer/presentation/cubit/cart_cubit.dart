import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/network/api_error_ui_message.dart';
import '../../domain/models/active_cart.dart';
import '../../domain/repositories/customer_cart_repository.dart';
import 'cart_state.dart';

class CartCubit extends Cubit<CartState> {
  CartCubit({required CustomerCartRepository repository})
    : _repository = repository,
      super(const CartState.initial());

  final CustomerCartRepository _repository;

  Future<void> loadCart() async {
    if (state.isBusy) {
      return;
    }

    emit(
      state.copyWith(
        status: CartStatus.loading,
        clearError: true,
        clearInfo: true,
      ),
    );

    try {
      final cart = await _repository.getActiveCart();
      _emitCart(cart, infoMessage: null);
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to load your cart.',
      );
      emit(
        state.copyWith(
          status: CartStatus.failure,
          errorMessage: presentation.message,
        ),
      );
    }
  }

  Future<void> addItem({
    required String menuItemId,
    required int quantity,
    String? note,
  }) async {
    await _mutateCart(
      action:
          () => _repository.addItem(
            menuItemId: menuItemId,
            quantity: quantity,
            note: note,
          ),
      successMessage: 'Added to cart.',
    );
  }

  Future<void> updateItem({
    required String menuItemId,
    required int quantity,
    String? note,
  }) async {
    await _mutateCart(
      action:
          () => _repository.updateItem(
            menuItemId: menuItemId,
            quantity: quantity,
            note: note,
          ),
      successMessage: 'Cart updated.',
    );
  }

  Future<void> removeItem(String menuItemId) async {
    await _mutateCart(
      action: () => _repository.removeItem(menuItemId),
      successMessage: 'Item removed.',
    );
  }

  Future<void> clearCart() async {
    await _mutateCart(
      action: () => _repository.clearCart(),
      successMessage: 'Cart cleared.',
    );
  }

  void clearFeedback() {
    emit(state.copyWith(clearError: true, clearInfo: true));
  }

  Future<void> _mutateCart({
    required Future<ActiveCart> Function() action,
    required String successMessage,
  }) async {
    if (state.isBusy) {
      return;
    }

    emit(
      state.copyWith(
        status: CartStatus.updating,
        clearError: true,
        clearInfo: true,
      ),
    );

    try {
      final cart = await action();
      _emitCart(cart, infoMessage: successMessage);
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to update the cart.',
      );

      emit(
        state.copyWith(
          status: CartStatus.failure,
          errorMessage: presentation.message,
        ),
      );
    }
  }

  void _emitCart(ActiveCart cart, {String? infoMessage}) {
    emit(
      state.copyWith(
        status: cart.items.isEmpty ? CartStatus.empty : CartStatus.success,
        cart: cart,
        infoMessage: infoMessage,
        clearError: true,
      ),
    );
  }
}
