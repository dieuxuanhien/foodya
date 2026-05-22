import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/network/api_error_ui_message.dart';
import '../../domain/repositories/customer_cart_repository.dart';
import 'add_to_cart_state.dart';

class AddToCartCubit extends Cubit<AddToCartState> {
  AddToCartCubit({required CustomerCartRepository repository})
    : _repository = repository,
      super(const AddToCartState.initial());

  final CustomerCartRepository _repository;

  Future<void> addItem({
    required String menuItemId,
    required int quantity,
    String? note,
  }) async {
    if (state.isBusy) {
      return;
    }

    emit(
      state.copyWith(
        status: AddToCartStatus.adding,
        clearError: true,
        clearInfo: true,
      ),
    );

    try {
      await _repository.addItem(
        menuItemId: menuItemId,
        quantity: quantity,
        note: note,
      );
      emit(
        state.copyWith(
          status: AddToCartStatus.success,
          infoMessage: 'Added to cart.',
          clearError: true,
        ),
      );
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to add item to cart.',
      );
      emit(
        state.copyWith(
          status: AddToCartStatus.failure,
          errorMessage: presentation.message,
        ),
      );
    }
  }

  void clearFeedback() {
    emit(
      state.copyWith(
        status: AddToCartStatus.idle,
        clearError: true,
        clearInfo: true,
      ),
    );
  }
}
