import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/formatters/vnd_currency_formatter.dart';
import '../../domain/models/delivery_models.dart';
import '../../domain/repositories/delivery_repository.dart';
import '../cubit/delivery_order_cubit.dart';
import '../cubit/delivery_order_state.dart';
import '../widgets/delivery_prototype_widgets.dart';

class DeliveryOrderPage extends StatelessWidget {
  const DeliveryOrderPage({super.key, required this.orderId, this.seed});
  final String orderId;
  final DeliveryAssignment? seed;

  @override
  Widget build(BuildContext context) => BlocProvider(
    create:
        (context) =>
            DeliveryOrderCubit(context.read<DeliveryRepository>())
              ..load(orderId, seed: seed),
    child: _OrderView(orderId: orderId, seed: seed),
  );
}

class _OrderView extends StatelessWidget {
  const _OrderView({required this.orderId, this.seed});
  final String orderId;
  final DeliveryAssignment? seed;

  @override
  Widget build(BuildContext context) =>
      BlocConsumer<DeliveryOrderCubit, DeliveryOrderState>(
        listenWhen:
            (previous, current) =>
                previous.errorMessage != current.errorMessage ||
                previous.infoMessage != current.infoMessage,
        listener: (context, state) {
          final message = state.errorMessage ?? state.infoMessage;
          if (message == null) return;
          ScaffoldMessenger.of(context)
            ..hideCurrentSnackBar()
            ..showSnackBar(SnackBar(content: Text(message)));
          context.read<DeliveryOrderCubit>().clearFeedback();
        },
        builder:
            (context, state) => Scaffold(
              appBar: AppBar(title: const Text('Delivery Order')),
              body: DeliveryPrototypeBody(child: _content(context, state)),
            ),
      );

  Widget _content(BuildContext context, DeliveryOrderState state) {
    if (state.status == DeliveryOrderViewStatus.initial ||
        state.status == DeliveryOrderViewStatus.loading) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(48),
          child: CircularProgressIndicator(),
        ),
      );
    }
    if (state.status == DeliveryOrderViewStatus.failure ||
        state.order == null) {
      return DeliveryErrorCard(
        message: state.errorMessage ?? 'Unable to load this order.',
        onRetry:
            () => context.read<DeliveryOrderCubit>().load(orderId, seed: seed),
      );
    }
    final order = state.order!;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        order.orderCode,
                        style: Theme.of(context).textTheme.titleLarge,
                      ),
                    ),
                    Chip(label: Text(order.status.label)),
                  ],
                ),
                Text(order.restaurantName),
                Text('Customer: ${order.customerName}'),
                Text(
                  'Payment: ${order.paymentMethod ?? 'Unavailable'} / ${order.paymentStatus}',
                ),
                const SizedBox(height: 8),
                Text(formatVndCurrency(order.totalAmount)),
                const Divider(height: 24),
                Text(
                  order.deliveryAddress ??
                      'Delivery address unavailable until a write response returns detail.',
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        _RouteCard(state: state),
        const SizedBox(height: 12),
        _Actions(order: order, busy: state.isBusy),
        if (state.isBusy) ...[
          const SizedBox(height: 12),
          const LinearProgressIndicator(),
        ],
      ],
    );
  }
}

class _RouteCard extends StatelessWidget {
  const _RouteCard({required this.state});
  final DeliveryOrderState state;

  @override
  Widget build(BuildContext context) {
    final route = state.route;
    return Card(
      child: ListTile(
        leading: const Icon(Icons.route_outlined),
        title: const Text('Planned route summary'),
        subtitle: Text(
          route == null
              ? 'Route unavailable. Navigation is deferred to Phase 6.'
              : '${(route.distanceMeters / 1000).toStringAsFixed(1)} km · ${(route.durationSeconds / 60).round()} min · ${route.waypoints.length} waypoints',
        ),
      ),
    );
  }
}

class _Actions extends StatelessWidget {
  const _Actions({required this.order, required this.busy});
  final DeliveryAssignment order;
  final bool busy;

  @override
  Widget build(BuildContext context) {
    final canStart =
        order.status == DeliveryOrderStatus.assigned ||
        order.status == DeliveryOrderStatus.preparing;
    final canFinish = order.status == DeliveryOrderStatus.delivering;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            FilledButton.tonal(
              onPressed:
                  busy || !canStart
                      ? null
                      : context.read<DeliveryOrderCubit>().acknowledge,
              child: const Text('Acknowledge'),
            ),
            FilledButton(
              onPressed:
                  busy || !canStart
                      ? null
                      : () => context.read<DeliveryOrderCubit>().updateStatus(
                        DeliveryOrderStatus.delivering,
                      ),
              child: const Text('Start delivery'),
            ),
            FilledButton(
              onPressed:
                  busy || !canFinish
                      ? null
                      : () => _confirmStatus(
                        context,
                        DeliveryOrderStatus.success,
                        'Mark this order as delivered?',
                      ),
              child: const Text('Mark delivered'),
            ),
            OutlinedButton(
              onPressed:
                  busy || !canFinish
                      ? null
                      : () => _confirmStatus(
                        context,
                        DeliveryOrderStatus.failed,
                        'Report this delivery as failed? The current API cannot store a reason.',
                      ),
              child: const Text('Report failure'),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _confirmStatus(
    BuildContext context,
    DeliveryOrderStatus status,
    String message,
  ) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder:
          (context) => AlertDialog(
            title: const Text('Confirm status update'),
            content: Text(message),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context, false),
                child: const Text('Cancel'),
              ),
              FilledButton(
                onPressed: () => Navigator.pop(context, true),
                child: const Text('Confirm'),
              ),
            ],
          ),
    );
    if (confirmed == true && context.mounted) {
      await context.read<DeliveryOrderCubit>().updateStatus(status);
    }
  }
}
