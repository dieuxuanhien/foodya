import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../domain/models/merchant_order_detail.dart';
import '../../domain/models/merchant_order_summary.dart';
import '../../domain/models/merchant_restaurant.dart';
import '../../domain/repositories/merchant_order_repository.dart';
import '../../domain/repositories/merchant_restaurant_repository.dart';
import '../cubit/merchant_orders_cubit.dart';
import '../cubit/merchant_orders_state.dart';

class MerchantOrdersPage extends StatelessWidget {
  const MerchantOrdersPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create:
          (context) => MerchantOrdersCubit(
            orderRepository: context.read<MerchantOrderRepository>(),
            restaurantRepository: context.read<MerchantRestaurantRepository>(),
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
              _RestaurantPicker(
                restaurants: state.restaurants,
                selectedRestaurant: state.selectedRestaurant,
                isLoading: state.isLoading,
                onSelected:
                    state.isBusy
                        ? null
                        : (restaurant) => context
                            .read<MerchantOrdersCubit>()
                            .loadRestaurantOrders(
                              restaurant,
                              clearSelection: true,
                            ),
              ),
              const SizedBox(height: 16),
              if (state.selectedRestaurant == null)
                const Card(
                  child: ListTile(
                    leading: Icon(Icons.storefront_outlined),
                    title: Text('No restaurant selected'),
                    subtitle: Text('Create a restaurant profile first.'),
                  ),
                )
              else ...[
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

class _RestaurantPicker extends StatelessWidget {
  const _RestaurantPicker({
    required this.restaurants,
    required this.selectedRestaurant,
    required this.isLoading,
    required this.onSelected,
  });

  final List<MerchantRestaurant> restaurants;
  final MerchantRestaurant? selectedRestaurant;
  final bool isLoading;
  final ValueChanged<MerchantRestaurant>? onSelected;

  @override
  Widget build(BuildContext context) {
    if (isLoading && restaurants.isEmpty) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.symmetric(vertical: 24),
          child: CircularProgressIndicator(),
        ),
      );
    }
    return DropdownButtonFormField<String>(
      value: selectedRestaurant?.id,
      decoration: const InputDecoration(labelText: 'Restaurant'),
      items: restaurants
          .map(
            (restaurant) => DropdownMenuItem(
              value: restaurant.id,
              child: Text(restaurant.name),
            ),
          )
          .toList(growable: false),
      onChanged:
          onSelected == null
              ? null
              : (id) {
                final matches = restaurants.where((item) => item.id == id);
                final restaurant = matches.isEmpty ? null : matches.first;
                if (restaurant != null) {
                  onSelected!(restaurant);
                }
              },
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
      return const Card(
        child: ListTile(
          leading: Icon(Icons.receipt_long_outlined),
          title: Text('No orders yet'),
          subtitle: Text('Incoming orders will appear here.'),
        ),
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
              leading: const Icon(Icons.receipt_long_outlined),
              title: Text(order.orderCode),
              subtitle: Text('${order.customerName} - ${order.status}'),
              trailing: Text(order.totalAmount.toStringAsFixed(0)),
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
            _InfoRow(label: 'Status', value: order.status),
            _InfoRow(
              label: 'Payment',
              value: _paymentStatusLabel(order.paymentStatus),
            ),
            _InfoRow(
              label: 'Subtotal',
              value: order.subtotalAmount.toStringAsFixed(0),
            ),
            _InfoRow(
              label: 'Delivery',
              value: order.deliveryFee.toStringAsFixed(0),
            ),
            _InfoRow(
              label: 'Total',
              value: order.totalAmount.toStringAsFixed(0),
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
  const _InfoRow({required this.label, required this.value});

  final String label;
  final String value;

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
          Expanded(child: Text(value.isEmpty ? '-' : value)),
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
