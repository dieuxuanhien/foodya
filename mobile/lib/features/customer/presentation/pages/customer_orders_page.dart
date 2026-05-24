import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../domain/repositories/customer_order_repository.dart';
import '../cubit/order_list_cubit.dart';
import '../cubit/order_list_state.dart';

class CustomerOrdersPage extends StatelessWidget {
  const CustomerOrdersPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create:
          (context) => OrderListCubit(
            repository: context.read<CustomerOrderRepository>(),
          )..loadOrders(),
      child: const _CustomerOrdersView(),
    );
  }
}

class _CustomerOrdersView extends StatelessWidget {
  const _CustomerOrdersView();

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<OrderListCubit, OrderListState>(
      builder: (context, state) {
        return Scaffold(
          appBar: AppBar(
            title: const Text('My Orders'),
            actions: [
              IconButton(
                onPressed:
                    state.isBusy
                        ? null
                        : () => context.read<OrderListCubit>().loadOrders(),
                icon: const Icon(Icons.refresh),
              ),
            ],
          ),
          body:
              state.status == OrderListStatus.loading
                  ? const Center(child: CircularProgressIndicator())
                  : state.status == OrderListStatus.failure
                  ? _EmptyState(
                    title: 'Unable to load orders',
                    subtitle: state.errorMessage ?? 'Please try again.',
                  )
                  : state.status == OrderListStatus.empty
                  ? const _EmptyState(
                    title: 'No orders yet',
                    subtitle: 'Place an order to see it here.',
                  )
                  : ListView.separated(
                    padding: const EdgeInsets.all(16),
                    itemCount: state.orders.length,
                    separatorBuilder: (_, __) => const SizedBox(height: 12),
                    itemBuilder: (context, index) {
                      final order = state.orders[index];
                      return Card(
                        child: ListTile(
                          title: Text(order.restaurantName),
                          subtitle: Text(
                            'Order ${order.orderCode} • ${order.status}',
                          ),
                          trailing: Text(
                            '${order.totalAmount.toStringAsFixed(0)} VND',
                            style: Theme.of(context).textTheme.labelLarge,
                          ),
                          onTap:
                              () => context.push(
                                '/customer/orders/${order.orderId}',
                              ),
                        ),
                      );
                    },
                  ),
        );
      },
    );
  }
}

class _EmptyState extends StatelessWidget {
  const _EmptyState({required this.title, required this.subtitle});

  final String title;
  final String subtitle;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.receipt_long_outlined, size: 36),
            const SizedBox(height: 12),
            Text(title, style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 6),
            Text(
              subtitle,
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.bodyMedium,
            ),
          ],
        ),
      ),
    );
  }
}
