import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/formatters/vnd_currency_formatter.dart';
import '../../../../core/ui/foodya_ui.dart';
import '../../domain/models/merchant_order_detail.dart';
import '../../domain/models/merchant_order_summary.dart';
import '../../domain/models/merchant_restaurant.dart';
import '../../domain/repositories/merchant_order_repository.dart';
import '../../domain/repositories/merchant_restaurant_repository.dart';
import '../cubit/merchant_orders_cubit.dart';
import '../cubit/merchant_orders_state.dart';
import '../cubit/merchant_restaurant_selection_cubit.dart';

class MerchantOrdersPage extends StatelessWidget {
  const MerchantOrdersPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create:
          (context) => MerchantOrdersCubit(
            orderRepository: context.read<MerchantOrderRepository>(),
            restaurantRepository: context.read<MerchantRestaurantRepository>(),
            selectionCubit: context.read<MerchantRestaurantSelectionCubit>(),
          )..load(),
      child: const _MerchantOrdersView(),
    );
  }
}

class _MerchantOrdersView extends StatelessWidget {
  const _MerchantOrdersView();

  @override
  Widget build(BuildContext context) {
    return BlocConsumer<MerchantOrdersCubit, MerchantOrdersState>(
      listenWhen:
          (previous, current) =>
              previous.errorMessage != current.errorMessage ||
              previous.infoMessage != current.infoMessage,
      listener: (context, state) {
        final message = state.errorMessage ?? state.infoMessage;
        if (message != null) {
          ScaffoldMessenger.of(
            context,
          ).showSnackBar(SnackBar(content: Text(message)));
          context.read<MerchantOrdersCubit>().clearFeedback();
        }
      },
      builder: (context, state) {
        return Scaffold(
          appBar: AppBar(
            title: const Text('Order Operations'),
            actions: [
              IconButton(
                onPressed:
                    state.isBusy
                        ? null
                        : () => context.read<MerchantOrdersCubit>().load(),
                icon: const Icon(Icons.refresh_outlined),
                tooltip: 'Refresh',
              ),
            ],
          ),
          body: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              if (state.selectedRestaurant == null)
                const FoodyaEmptyState(
                  illustrationAsset:
                      'assets/illustrations/empty_storefront.png',
                  icon: Icons.storefront_outlined,
                  title: 'No restaurant selected',
                  message: 'Create a restaurant profile first.',
                )
              else ...[
                _ActiveRestaurantCard(restaurant: state.selectedRestaurant!),
                const SizedBox(height: 16),
                _OrderList(
                  orders: state.orders,
                  selectedOrderId: state.selectedOrder?.orderId,
                  isLoading: state.isLoading,
                  isBusy: state.isBusy,
                  onSelected:
                      (order) => context
                          .read<MerchantOrdersCubit>()
                          .selectOrder(order),
                ),
                const SizedBox(height: 16),
                if (state.selectedOrder != null)
                  _OrderDetailCard(
                    order: state.selectedOrder!,
                    isBusy: state.isBusy,
                    onAccept:
                        () => context.read<MerchantOrdersCubit>().updateStatus(
                          'ACCEPTED',
                        ),
                    onPreparing:
                        () => context.read<MerchantOrdersCubit>().updateStatus(
                          'PREPARING',
                        ),
                  ),
              ],
            ],
          ),
        );
      },
    );
  }
}

class _ActiveRestaurantCard extends StatelessWidget {
  const _ActiveRestaurantCard({required this.restaurant});

  final MerchantRestaurant restaurant;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ListTile(
        leading: const Icon(Icons.storefront_outlined),
        title: Text(restaurant.name),
        subtitle: Text(restaurant.cuisineType),
        trailing: FoodyaStatusChip(value: restaurant.status),
      ),
    );
  }
}

class _OrderList extends StatelessWidget {
  const _OrderList({
    required this.orders,
    required this.selectedOrderId,
    required this.isLoading,
    required this.isBusy,
    required this.onSelected,
  });

  final List<MerchantOrderSummary> orders;
  final String? selectedOrderId;
  final bool isLoading;
  final bool isBusy;
  final ValueChanged<MerchantOrderSummary> onSelected;

  @override
  Widget build(BuildContext context) {
    if (isLoading && orders.isEmpty) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.symmetric(vertical: 24),
          child: CircularProgressIndicator(),
        ),
      );
    }
    if (orders.isEmpty) {
      return const FoodyaEmptyState(
        illustrationAsset: 'assets/illustrations/empty_orders.png',
        icon: Icons.receipt_long_outlined,
        title: 'No orders yet',
        message: 'Incoming orders will appear here.',
      );
    }
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text('Incoming Orders', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 8),
        for (final order in orders)
          Card(
            child: ListTile(
              selected: selectedOrderId == order.orderId,
              onTap: isBusy ? null : () => onSelected(order),
              leading: CircleAvatar(
                backgroundColor: const Color(0xFFFFEDD5),
                foregroundColor: const Color(0xFFEA580C),
                child: Icon(orderStatusIcon(order.status)),
              ),
              title: Text(order.orderCode),
              subtitle: Text(
                '${order.customerName} · ${formatVndCurrency(order.totalAmount)}',
              ),
              trailing: FoodyaStatusChip(value: order.status),
            ),
          ),
      ],
    );
  }
}

class _OrderDetailCard extends StatelessWidget {
  const _OrderDetailCard({
    required this.order,
    required this.isBusy,
    required this.onAccept,
    required this.onPreparing,
  });

  final MerchantOrderDetail order;
  final bool isBusy;
  final VoidCallback onAccept;
  final VoidCallback onPreparing;

  @override
  Widget build(BuildContext context) {
    final canAccept = order.status == 'PENDING';
    final canPrepare = order.status == 'ASSIGNED';
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              order.orderCode,
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            _InfoRow(label: 'Customer', value: order.customerName),
            _InfoRow(label: 'Restaurant', value: order.restaurantName),
            _InfoRow(
              label: 'Status',
              valueWidget: Align(
                alignment: Alignment.centerLeft,
                child: FoodyaStatusChip(value: order.status),
              ),
            ),
            _InfoRow(
              label: 'Payment',
              value: _paymentStatusLabel(order.paymentStatus),
            ),
            _InfoRow(
              label: 'Subtotal',
              value: formatVndCurrency(order.subtotalAmount),
            ),
            _InfoRow(
              label: 'Delivery',
              value: formatVndCurrency(order.deliveryFee),
            ),
            _InfoRow(
              label: 'Total',
              value: formatVndCurrency(order.totalAmount),
            ),
            _InfoRow(label: 'Address', value: order.deliveryAddress),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: FilledButton.icon(
                    onPressed: isBusy || !canAccept ? null : onAccept,
                    icon: const Icon(Icons.check_circle_outline),
                    label: const Text('Accept'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: FilledButton.tonalIcon(
                    onPressed: isBusy || !canPrepare ? null : onPreparing,
                    icon: const Icon(Icons.local_fire_department_outlined),
                    label: const Text('Preparing'),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  const _InfoRow({required this.label, this.value = '', this.valueWidget});

  final String label;
  final String value;
  final Widget? valueWidget;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 92,
            child: Text(label, style: Theme.of(context).textTheme.labelMedium),
          ),
          Expanded(child: valueWidget ?? Text(value.isEmpty ? '-' : value)),
        ],
      ),
    );
  }
}

String _paymentStatusLabel(String value) {
  return switch (value.toUpperCase()) {
    'UNPAID' => 'COD pending',
    'PAID' => 'COD collected',
    'FAILED' => 'Payment failed',
    'REFUNDED' => 'Payment refunded',
    _ => value,
  };
}
