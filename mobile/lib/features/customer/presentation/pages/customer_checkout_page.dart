import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/formatters/vnd_currency_formatter.dart';
import '../../../../core/location/geolocation_service.dart';
import '../../domain/models/active_cart.dart';
import '../../domain/models/order_cost_review.dart';
import '../../domain/repositories/customer_cart_repository.dart';
import '../../domain/repositories/customer_order_repository.dart';
import '../cubit/checkout_cubit.dart';
import '../cubit/checkout_state.dart';
import '../widgets/manual_location_sheet.dart';

class CustomerCheckoutPage extends StatelessWidget {
  const CustomerCheckoutPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create:
          (context) => CheckoutCubit(
            cartRepository: context.read<CustomerCartRepository>(),
            orderRepository: context.read<CustomerOrderRepository>(),
            geolocationService: context.read<GeolocationService>(),
          )..loadCart(),
      child: const _CheckoutView(),
    );
  }
}

class _CheckoutView extends StatefulWidget {
  const _CheckoutView();

  @override
  State<_CheckoutView> createState() => _CheckoutViewState();
}

class _CheckoutViewState extends State<_CheckoutView> {
  final TextEditingController _addressController = TextEditingController();
  final TextEditingController _noteController = TextEditingController();

  @override
  void dispose() {
    _addressController.dispose();
    _noteController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return BlocConsumer<CheckoutCubit, CheckoutState>(
      listenWhen:
          (previous, current) =>
              previous.errorMessage != current.errorMessage ||
              previous.infoMessage != current.infoMessage ||
              previous.orderCreated != current.orderCreated,
      listener: (context, state) {
        if (state.orderCreated != null) {
          context.push('/customer/orders/${state.orderCreated!.orderId}');
        }

        final message = state.errorMessage ?? state.infoMessage;
        if (message == null) {
          return;
        }
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(message)));
        context.read<CheckoutCubit>().clearFeedback();
      },
      builder: (context, state) {
        if (_addressController.text != state.deliveryAddress) {
          _addressController.text = state.deliveryAddress;
          _addressController.selection = TextSelection.fromPosition(
            TextPosition(offset: _addressController.text.length),
          );
        }
        if (_noteController.text != state.customerNote) {
          _noteController.text = state.customerNote;
          _noteController.selection = TextSelection.fromPosition(
            TextPosition(offset: _noteController.text.length),
          );
        }

        final cart = state.cart;

        return Scaffold(
          appBar: AppBar(title: const Text('Checkout')),
          body:
              state.status == CheckoutStatus.loading && cart == null
                  ? const Center(child: CircularProgressIndicator())
                  : state.status == CheckoutStatus.failure && cart == null
                  ? _EmptyCheckoutState(
                    title: 'Unable to load checkout',
                    subtitle: state.errorMessage ?? 'Please try again.',
                    showRetry: true,
                    onRetry: () => context.read<CheckoutCubit>().loadCart(),
                  )
                  : cart == null || cart.items.isEmpty
                  ? const _EmptyCheckoutState()
                  : ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      Text(
                        cart.restaurantName,
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(height: 4),
                      Text('${cart.itemCount} items'),
                      const SizedBox(height: 16),
                      _CartItemsSummary(cart: cart),
                      const SizedBox(height: 16),
                      Text(
                        'Delivery details',
                        style: Theme.of(context).textTheme.titleSmall,
                      ),
                      const SizedBox(height: 8),
                      TextField(
                        controller: _addressController,
                        onChanged:
                            (value) => context
                                .read<CheckoutCubit>()
                                .updateDeliveryAddress(value),
                        decoration: const InputDecoration(
                          labelText: 'Delivery address',
                          hintText: 'Street, district, city',
                        ),
                        textInputAction: TextInputAction.next,
                      ),
                      const SizedBox(height: 12),
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              state.hasLocation
                                  ? 'Lat ${state.deliveryLatitude!.toStringAsFixed(5)}, Lng ${state.deliveryLongitude!.toStringAsFixed(5)}'
                                  : 'Location not set',
                              style: Theme.of(context).textTheme.bodySmall,
                            ),
                          ),
                          TextButton.icon(
                            onPressed:
                                state.isBusy
                                    ? null
                                    : () =>
                                        _chooseDeliveryLocation(context, state),
                            icon: const Icon(Icons.my_location_outlined),
                            label: const Text('Set location'),
                          ),
                        ],
                      ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: _noteController,
                        onChanged:
                            (value) => context
                                .read<CheckoutCubit>()
                                .updateCustomerNote(value),
                        decoration: const InputDecoration(
                          labelText: 'Order note (optional)',
                          hintText: 'e.g., Leave at the front desk',
                        ),
                        maxLines: 2,
                      ),
                      const SizedBox(height: 16),
                      _CostReviewSection(review: state.costReview),
                      const SizedBox(height: 12),
                      FilledButton(
                        onPressed:
                            state.isBusy
                                ? null
                                : () =>
                                    context.read<CheckoutCubit>().reviewCost(),
                        child:
                            state.status == CheckoutStatus.reviewing
                                ? const SizedBox(
                                  width: 18,
                                  height: 18,
                                  child: CircularProgressIndicator(
                                    strokeWidth: 2.5,
                                  ),
                                )
                                : const Text('Review cost'),
                      ),
                      const SizedBox(height: 8),
                      FilledButton.tonal(
                        onPressed:
                            state.isBusy
                                ? null
                                : () =>
                                    context.read<CheckoutCubit>().submitOrder(),
                        child:
                            state.status == CheckoutStatus.submitting
                                ? const SizedBox(
                                  width: 18,
                                  height: 18,
                                  child: CircularProgressIndicator(
                                    strokeWidth: 2.5,
                                  ),
                                )
                                : const Text('Place order'),
                      ),
                    ],
                  ),
        );
      },
    );
  }

  Future<void> _chooseDeliveryLocation(
    BuildContext context,
    CheckoutState state,
  ) async {
    final source = await showModalBottomSheet<String>(
      context: context,
      builder:
          (context) => SafeArea(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                ListTile(
                  leading: const Icon(Icons.my_location_outlined),
                  title: const Text('Use device location'),
                  onTap: () => Navigator.of(context).pop('device'),
                ),
                ListTile(
                  leading: const Icon(Icons.edit_location_alt_outlined),
                  title: const Text('Enter latitude and longitude'),
                  onTap: () => Navigator.of(context).pop('manual'),
                ),
              ],
            ),
          ),
    );
    if (!context.mounted || source == null) {
      return;
    }

    final cubit = context.read<CheckoutCubit>();
    if (source == 'device') {
      await cubit.useCurrentLocation();
      return;
    }

    final manual = await showManualLocationSheet(
      context: context,
      initialLatitude: state.deliveryLatitude,
      initialLongitude: state.deliveryLongitude,
    );
    if (!context.mounted || manual == null) {
      return;
    }
    await cubit.updateLocation(lat: manual.latitude, lng: manual.longitude);
  }
}

class _CartItemsSummary extends StatelessWidget {
  const _CartItemsSummary({required this.cart});

  final ActiveCart cart;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            ...cart.items.map(
              (item) => Row(
                children: [
                  Expanded(child: Text(item.menuItemName)),
                  Text('x${item.quantity}'),
                  const SizedBox(width: 12),
                  Text(formatVndCurrency(item.lineTotal)),
                ],
              ),
            ),
            const Divider(height: 20),
            Row(
              children: [
                const Text('Subtotal'),
                const Spacer(),
                Text(formatVndCurrency(cart.subtotal)),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _CostReviewSection extends StatelessWidget {
  const _CostReviewSection({required this.review});

  final OrderCostReview? review;

  @override
  Widget build(BuildContext context) {
    final currentReview = review;
    if (currentReview == null) {
      return Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Text(
            'Review cost to see delivery fee and total.',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
        ),
      );
    }

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            _CostRow(label: 'Subtotal', value: currentReview.subtotalAmount),
            _CostRow(label: 'Delivery fee', value: currentReview.deliveryFee),
            const Divider(height: 20),
            _CostRow(
              label: 'Total',
              value: currentReview.totalAmount,
              isEmphasis: true,
            ),
          ],
        ),
      ),
    );
  }
}

class _CostRow extends StatelessWidget {
  const _CostRow({
    required this.label,
    required this.value,
    this.isEmphasis = false,
  });

  final String label;
  final double value;
  final bool isEmphasis;

  @override
  Widget build(BuildContext context) {
    final style =
        isEmphasis
            ? Theme.of(context).textTheme.titleMedium
            : Theme.of(context).textTheme.bodyMedium;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          Text(label, style: style),
          const Spacer(),
          Text(formatVndCurrency(value), style: style),
        ],
      ),
    );
  }
}

class _EmptyCheckoutState extends StatelessWidget {
  const _EmptyCheckoutState({
    this.title = 'Your cart is empty',
    this.subtitle = 'Add items to your cart before checking out.',
    this.showRetry = false,
    this.onRetry,
  });

  final String title;
  final String subtitle;
  final bool showRetry;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.shopping_bag_outlined, size: 36),
            const SizedBox(height: 12),
            Text(title, style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 6),
            Text(
              subtitle,
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.bodyMedium,
            ),
            if (showRetry) ...[
              const SizedBox(height: 12),
              FilledButton(onPressed: onRetry, child: const Text('Retry')),
            ] else ...[
              const SizedBox(height: 12),
              FilledButton(
                onPressed: () => context.pop(),
                child: const Text('Back to cart'),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
