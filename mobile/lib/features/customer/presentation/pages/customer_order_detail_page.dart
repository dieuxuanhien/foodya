import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../domain/repositories/customer_order_repository.dart';
import '../cubit/order_detail_cubit.dart';
import '../cubit/order_detail_state.dart';

class CustomerOrderDetailPage extends StatelessWidget {
  const CustomerOrderDetailPage({super.key, required this.orderId});

  final String orderId;

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create:
          (context) => OrderDetailCubit(
            repository: context.read<CustomerOrderRepository>(),
          )..load(orderId),
      child: const _CustomerOrderDetailView(),
    );
  }
}

class _CustomerOrderDetailView extends StatelessWidget {
  const _CustomerOrderDetailView();

  @override
  Widget build(BuildContext context) {
    return BlocConsumer<OrderDetailCubit, OrderDetailState>(
      listenWhen:
          (previous, current) =>
              previous.errorMessage != current.errorMessage ||
              previous.infoMessage != current.infoMessage,
      listener: (context, state) {
        final message = state.errorMessage ?? state.infoMessage;
        if (message == null) {
          return;
        }
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(message)),
        );
        context.read<OrderDetailCubit>().clearFeedback();
      },
      builder: (context, state) {
        final order = state.order;
        return Scaffold(
          appBar: AppBar(
            title: Text(order?.orderCode ?? 'Order Detail'),
            actions: [
              IconButton(
                onPressed:
                    state.isBusy
                        ? null
                        : () => context.read<OrderDetailCubit>().refreshTracking(),
                icon: const Icon(Icons.refresh),
              ),
            ],
          ),
          body:
              state.status == OrderDetailStatus.loading && order == null
                  ? const Center(child: CircularProgressIndicator())
                  : order == null
                  ? Center(
                    child: Padding(
                      padding: const EdgeInsets.all(24),
                      child: Text(
                        state.errorMessage ?? 'Unable to load order detail.',
                        textAlign: TextAlign.center,
                      ),
                    ),
                  )
                  : RefreshIndicator(
                    onRefresh:
                        () => context.read<OrderDetailCubit>().refreshTracking(),
                    child: ListView(
                      padding: const EdgeInsets.all(16),
                      children: [
                        Card(
                          child: Padding(
                            padding: const EdgeInsets.all(16),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  order.restaurantName,
                                  style:
                                      Theme.of(context).textTheme.titleMedium,
                                ),
                                const SizedBox(height: 8),
                                Wrap(
                                  spacing: 8,
                                  runSpacing: 8,
                                  children: [
                                    Chip(label: Text(order.status)),
                                    Chip(label: Text(order.paymentStatus)),
                                    Chip(label: Text(order.paymentMethod)),
                                  ],
                                ),
                                const SizedBox(height: 12),
                                Text(order.deliveryAddress),
                                const SizedBox(height: 8),
                                Text(
                                  'Subtotal ${order.subtotalAmount.toStringAsFixed(0)} VND',
                                ),
                                Text(
                                  'Delivery fee ${order.deliveryFee.toStringAsFixed(0)} VND',
                                ),
                                Text(
                                  'Total ${order.totalAmount.toStringAsFixed(0)} VND',
                                  style: Theme.of(context).textTheme.titleSmall,
                                ),
                              ],
                            ),
                          ),
                        ),
                        const SizedBox(height: 16),
                        Text(
                          'Tracking points',
                          style: Theme.of(context).textTheme.titleSmall,
                        ),
                        const SizedBox(height: 8),
                        if (state.isTrackingLoading && state.trackingPoints.isEmpty)
                          const Padding(
                            padding: EdgeInsets.all(24),
                            child: Center(child: CircularProgressIndicator()),
                          )
                        else if (state.trackingPoints.isEmpty)
                          const Card(
                            child: Padding(
                              padding: EdgeInsets.all(16),
                              child: Text('No tracking updates yet.'),
                            ),
                          )
                        else
                          ...state.trackingPoints.map(
                            (point) => Card(
                              child: ListTile(
                                leading: const Icon(Icons.location_on_outlined),
                                title: Text(
                                  '${point.lat.toStringAsFixed(5)}, ${point.lng.toStringAsFixed(5)}',
                                ),
                                subtitle: Text(point.recordedAt.toLocal().toString()),
                              ),
                            ),
                          ),
                        const SizedBox(height: 16),
                        if (order.status.toUpperCase() != 'CANCELLED')
                          FilledButton.tonal(
                            onPressed:
                                state.isBusy
                                    ? null
                                    : () => _cancelOrder(context),
                            child: const Text('Cancel order'),
                          ),
                      ],
                    ),
                  ),
        );
      },
    );
  }

  void _cancelOrder(BuildContext context) {
    showDialog<void>(
      context: context,
      builder:
          (dialogContext) => AlertDialog(
            title: const Text('Cancel order?'),
            content: const Text('You can optionally add a cancel reason.'),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(dialogContext).pop(),
                child: const Text('Keep order'),
              ),
              FilledButton(
                onPressed: () {
                  Navigator.of(dialogContext).pop();
                  context.read<OrderDetailCubit>().cancelOrder();
                },
                child: const Text('Cancel'),
              ),
            ],
          ),
    );
  }
}
