import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../domain/repositories/customer_cart_repository.dart';
import '../cubit/cart_cubit.dart';
import '../cubit/cart_state.dart';

class CustomerCartPage extends StatelessWidget {
  const CustomerCartPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create:
          (context) =>
              CartCubit(repository: context.read<CustomerCartRepository>())
                ..loadCart(),
      child: const _CustomerCartView(),
    );
  }
}

class _CustomerCartView extends StatelessWidget {
  const _CustomerCartView();

  @override
  Widget build(BuildContext context) {
    return BlocConsumer<CartCubit, CartState>(
      listenWhen:
          (previous, current) =>
              previous.errorMessage != current.errorMessage ||
              previous.infoMessage != current.infoMessage,
      listener: (context, state) {
        final message = state.errorMessage ?? state.infoMessage;
        if (message == null) {
          return;
        }
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(message)));
        context.read<CartCubit>().clearFeedback();
      },
      builder: (context, state) {
        final cart = state.cart;
        return Scaffold(
          appBar: AppBar(
            title: const Text('Your Cart'),
            actions: [
              if (cart != null && cart.items.isNotEmpty)
                IconButton(
                  onPressed: state.isBusy ? null : () => _confirmClear(context),
                  icon: const Icon(Icons.delete_outline),
                  tooltip: 'Clear cart',
                ),
            ],
          ),
          body:
              state.status == CartStatus.loading && cart == null
                  ? const Center(child: CircularProgressIndicator())
                  : state.status == CartStatus.failure && cart == null
                  ? _EmptyCartState(
                    title: 'Unable to load cart',
                    subtitle: state.errorMessage ?? 'Please try again.',
                    showRetry: true,
                    onRetry: () => context.read<CartCubit>().loadCart(),
                  )
                  : cart == null || cart.items.isEmpty
                  ? const _EmptyCartState()
                  : Column(
                    children: [
                      Expanded(
                        child: ListView.separated(
                          padding: const EdgeInsets.all(16),
                          itemCount: cart.items.length,
                          separatorBuilder:
                              (_, __) => const SizedBox(height: 12),
                          itemBuilder: (context, index) {
                            final item = cart.items[index];
                            return _CartItemCard(
                              itemName: item.menuItemName,
                              note: item.note,
                              unitPrice: item.unitPrice,
                              quantity: item.quantity,
                              lineTotal: item.lineTotal,
                              onIncrease:
                                  state.isBusy
                                      ? null
                                      : () =>
                                          context.read<CartCubit>().updateItem(
                                            menuItemId: item.menuItemId,
                                            quantity: item.quantity + 1,
                                            note: item.note,
                                          ),
                              onDecrease:
                                  state.isBusy
                                      ? null
                                      : () {
                                        final next = item.quantity - 1;
                                        if (next <= 0) {
                                          context.read<CartCubit>().removeItem(
                                            item.menuItemId,
                                          );
                                          return;
                                        }
                                        context.read<CartCubit>().updateItem(
                                          menuItemId: item.menuItemId,
                                          quantity: next,
                                          note: item.note,
                                        );
                                      },
                              onRemove:
                                  state.isBusy
                                      ? null
                                      : () => context
                                          .read<CartCubit>()
                                          .removeItem(item.menuItemId),
                            );
                          },
                        ),
                      ),
                      _CartSummary(
                        subtotal: cart.subtotal,
                        itemCount: cart.itemCount,
                        isBusy: state.isBusy,
                        onCheckout:
                            state.isBusy
                                ? null
                                : () => context.push('/customer/checkout'),
                      ),
                    ],
                  ),
        );
      },
    );
  }

  void _confirmClear(BuildContext context) {
    showDialog<void>(
      context: context,
      builder:
          (dialogContext) => AlertDialog(
            title: const Text('Clear cart?'),
            content: const Text('This removes all items from your cart.'),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(dialogContext).pop(),
                child: const Text('Cancel'),
              ),
              FilledButton(
                onPressed: () {
                  Navigator.of(dialogContext).pop();
                  context.read<CartCubit>().clearCart();
                },
                child: const Text('Clear'),
              ),
            ],
          ),
    );
  }
}

class _CartItemCard extends StatelessWidget {
  const _CartItemCard({
    required this.itemName,
    required this.unitPrice,
    required this.quantity,
    required this.lineTotal,
    required this.onIncrease,
    required this.onDecrease,
    required this.onRemove,
    this.note,
  });

  final String itemName;
  final String? note;
  final double unitPrice;
  final int quantity;
  final double lineTotal;
  final VoidCallback? onIncrease;
  final VoidCallback? onDecrease;
  final VoidCallback? onRemove;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    itemName,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),
                IconButton(
                  onPressed: onRemove,
                  icon: const Icon(Icons.close),
                  tooltip: 'Remove item',
                ),
              ],
            ),
            if (note != null && note!.trim().isNotEmpty) ...[
              const SizedBox(height: 4),
              Text(
                'Note: ${note!}',
                style: Theme.of(context).textTheme.bodySmall,
              ),
            ],
            const SizedBox(height: 8),
            Row(
              children: [
                Text(
                  '${unitPrice.toStringAsFixed(0)} VND',
                  style: Theme.of(context).textTheme.bodyMedium,
                ),
                const Spacer(),
                IconButton(
                  onPressed: onDecrease,
                  icon: const Icon(Icons.remove_circle_outline),
                ),
                Text(
                  quantity.toString(),
                  style: Theme.of(context).textTheme.titleSmall,
                ),
                IconButton(
                  onPressed: onIncrease,
                  icon: const Icon(Icons.add_circle_outline),
                ),
              ],
            ),
            const Divider(),
            Align(
              alignment: Alignment.centerRight,
              child: Text(
                'Line total: ${lineTotal.toStringAsFixed(0)} VND',
                style: Theme.of(context).textTheme.labelLarge,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _CartSummary extends StatelessWidget {
  const _CartSummary({
    required this.subtotal,
    required this.itemCount,
    required this.isBusy,
    required this.onCheckout,
  });

  final double subtotal;
  final int itemCount;
  final bool isBusy;
  final VoidCallback? onCheckout;

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      top: false,
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: Theme.of(context).colorScheme.surfaceContainerHighest,
          borderRadius: const BorderRadius.vertical(top: Radius.circular(16)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                Text(
                  '$itemCount items',
                  style: Theme.of(context).textTheme.bodyMedium,
                ),
                const Spacer(),
                Text(
                  'Subtotal: ${subtotal.toStringAsFixed(0)} VND',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
              ],
            ),
            const SizedBox(height: 12),
            FilledButton(
              onPressed: isBusy ? null : onCheckout,
              child: const Text('Proceed to Checkout'),
            ),
          ],
        ),
      ),
    );
  }
}

class _EmptyCartState extends StatelessWidget {
  const _EmptyCartState({
    this.title = 'Your cart is empty',
    this.subtitle = 'Browse restaurants and add items to start an order.',
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
            const Icon(Icons.shopping_cart_outlined, size: 36),
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
            ],
          ],
        ),
      ),
    );
  }
}
