import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:geolocator/geolocator.dart';

import '../../../../core/location/geolocation_service.dart';
import '../../../../core/network/api_error_ui_message.dart';
import '../../domain/models/create_order_request.dart';
import '../../domain/repositories/customer_cart_repository.dart';
import '../../domain/repositories/customer_order_repository.dart';
import 'checkout_state.dart';

class CheckoutCubit extends Cubit<CheckoutState> {
  CheckoutCubit({
    required CustomerCartRepository cartRepository,
    required CustomerOrderRepository orderRepository,
    required GeolocationService geolocationService,
  }) : _cartRepository = cartRepository,
       _orderRepository = orderRepository,
       _geolocationService = geolocationService,
       super(const CheckoutState.initial());

  final CustomerCartRepository _cartRepository;
  final CustomerOrderRepository _orderRepository;
  final GeolocationService _geolocationService;

  String? _idempotencyKey;

  Future<void> loadCart() async {
    if (state.isBusy) {
      return;
    }

    emit(
      state.copyWith(
        status: CheckoutStatus.loading,
        clearError: true,
        clearInfo: true,
      ),
    );

    try {
      final cart = await _cartRepository.getActiveCart();
      emit(
        state.copyWith(
          status: cart.items.isEmpty ? CheckoutStatus.empty : CheckoutStatus.ready,
          cart: cart,
          clearError: true,
          clearCostReview: true,
        ),
      );
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to load cart for checkout.',
      );
      emit(
        state.copyWith(
          status: CheckoutStatus.failure,
          errorMessage: presentation.message,
        ),
      );
    }
  }

  void updateDeliveryAddress(String value) {
    emit(state.copyWith(deliveryAddress: value));
  }

  void updateCustomerNote(String value) {
    emit(state.copyWith(customerNote: value));
  }

  void updateLocation({required double lat, required double lng}) {
    emit(state.copyWith(deliveryLatitude: lat, deliveryLongitude: lng));
  }

  Future<void> useCurrentLocation() async {
    if (state.isBusy) {
      return;
    }

    try {
      final enabled = await _geolocationService.isLocationServiceEnabled();
      if (!enabled) {
        emit(
          state.copyWith(
            errorMessage: 'Location service is turned off.',
            clearInfo: true,
          ),
        );
        return;
      }

      final permission = await _geolocationService.requestPermission();
      if (permission == LocationPermission.denied ||
          permission == LocationPermission.deniedForever) {
        emit(
          state.copyWith(
            errorMessage: 'Location permission denied.',
            clearInfo: true,
          ),
        );
        return;
      }

      final position = await _geolocationService.getCurrentPosition(
        accuracy: LocationAccuracy.high,
        timeLimit: const Duration(seconds: 10),
      );
      final friendly = await _geolocationService.getFriendlyAddress(position);

      emit(
        state.copyWith(
          deliveryLatitude: position.latitude,
          deliveryLongitude: position.longitude,
          deliveryAddress:
              friendly != null && friendly.trim().isNotEmpty
                  ? friendly
                  : state.deliveryAddress,
          infoMessage: 'Delivery location updated.',
          clearError: true,
        ),
      );
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to resolve your location.',
      );
      emit(
        state.copyWith(
          errorMessage: presentation.message,
          clearInfo: true,
        ),
      );
    }
  }

  Future<void> reviewCost() async {
    if (state.isBusy) {
      return;
    }

    final request = _buildRequest();
    if (request == null) {
      return;
    }

    emit(
      state.copyWith(
        status: CheckoutStatus.reviewing,
        clearError: true,
        clearInfo: true,
      ),
    );

    try {
      final review = await _orderRepository.reviewOrderCost(request);
      emit(
        state.copyWith(
          status: CheckoutStatus.ready,
          costReview: review,
          clearError: true,
        ),
      );
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to review order cost.',
      );
      emit(
        state.copyWith(
          status: CheckoutStatus.failure,
          errorMessage: presentation.message,
        ),
      );
    }
  }

  Future<void> submitOrder() async {
    if (state.isBusy) {
      return;
    }

    final request = _buildRequest();
    if (request == null) {
      return;
    }

    _idempotencyKey ??= _generateIdempotencyKey();

    emit(
      state.copyWith(
        status: CheckoutStatus.submitting,
        clearError: true,
        clearInfo: true,
      ),
    );

    try {
      final created = await _orderRepository.createOrder(
        request: request,
        idempotencyKey: _idempotencyKey!,
      );
      emit(
        state.copyWith(
          status: CheckoutStatus.success,
          orderCreated: created,
          infoMessage: 'Order created successfully.',
          clearError: true,
        ),
      );
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to place order.',
      );
      emit(
        state.copyWith(
          status: CheckoutStatus.failure,
          errorMessage: presentation.message,
        ),
      );
    }
  }

  void clearFeedback() {
    emit(state.copyWith(clearError: true, clearInfo: true));
  }

  CreateOrderRequest? _buildRequest() {
    final cart = state.cart;
    if (cart == null || cart.items.isEmpty) {
      emit(
        state.copyWith(
          status: CheckoutStatus.empty,
          errorMessage: 'Your cart is empty.',
        ),
      );
      return null;
    }

    if (state.deliveryAddress.trim().isEmpty) {
      emit(
        state.copyWith(
          errorMessage: 'Delivery address is required.',
          clearInfo: true,
        ),
      );
      return null;
    }

    if (!state.hasLocation) {
      emit(
        state.copyWith(
          errorMessage: 'Delivery location is required.',
          clearInfo: true,
        ),
      );
      return null;
    }

    return CreateOrderRequest(
      deliveryAddress: state.deliveryAddress.trim(),
      deliveryLatitude: state.deliveryLatitude!,
      deliveryLongitude: state.deliveryLongitude!,
      customerNote:
          state.customerNote.trim().isEmpty ? null : state.customerNote.trim(),
    );
  }

  String _generateIdempotencyKey() {
    final cartId = state.cart?.cartId ?? 'cart';
    return 'cart-$cartId-${DateTime.now().microsecondsSinceEpoch}';
  }
}
