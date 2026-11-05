import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../domain/models/delivery_models.dart';
import '../../domain/repositories/delivery_repository.dart';
import '../cubit/delivery_batch_cubit.dart';
import '../cubit/delivery_batch_state.dart';
import '../widgets/delivery_prototype_widgets.dart';

class DeliveryBatchManagementPage extends StatelessWidget {
  const DeliveryBatchManagementPage({super.key, required this.batchId});
  final String batchId;

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create:
          (context) =>
              DeliveryBatchCubit(context.read<DeliveryRepository>())
                ..load(batchId),
      child: _BatchManagementView(batchId: batchId),
    );
  }
}

class _BatchManagementView extends StatelessWidget {
  const _BatchManagementView({required this.batchId});
  final String batchId;

  @override
  Widget build(BuildContext context) {
    return BlocConsumer<DeliveryBatchCubit, DeliveryBatchState>(
      listenWhen:
          (a, b) =>
              a.errorMessage != b.errorMessage ||
              a.infoMessage != b.infoMessage,
      listener: (context, state) {
        final message = state.errorMessage ?? state.infoMessage;
        if (message == null) return;
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(message)));
        context.read<DeliveryBatchCubit>().clearFeedback();
      },
      builder:
          (context, state) => Scaffold(
            appBar: AppBar(title: const Text('Batch Management')),
            body: DeliveryPrototypeBody(child: _content(context, state)),
          ),
    );
  }

  Widget _content(BuildContext context, DeliveryBatchState state) {
    if (state.status == DeliveryBatchViewStatus.loading ||
        state.status == DeliveryBatchViewStatus.initial) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(48),
          child: CircularProgressIndicator(),
        ),
      );
    }
    if (state.status == DeliveryBatchViewStatus.failure &&
        state.batch == null) {
      return DeliveryErrorCard(
        message: state.errorMessage ?? 'Unable to load batch.',
        onRetry: () => context.read<DeliveryBatchCubit>().load(batchId),
      );
    }
    final batch = state.batch;
    if (batch == null) return const Text('No active batch to manage.');
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(batch.batchId, style: Theme.of(context).textTheme.titleLarge),
        const SizedBox(height: 12),
        const _ProgressHeader(),
        const SizedBox(height: 18),
        ...batch.stops.map(
          (stop) => Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: _ManageStopCard(stop: stop, busy: state.isBusy),
          ),
        ),
      ],
    );
  }
}

class _ProgressHeader extends StatelessWidget {
  const _ProgressHeader();
  @override
  Widget build(BuildContext context) => Card(
    child: Padding(
      padding: const EdgeInsets.all(16),
      child: Row(
        children: const [
          Expanded(
            child: _ProgressStep(icon: Icons.storefront, label: 'Pickup'),
          ),
          Icon(Icons.chevron_right),
          Expanded(
            child: _ProgressStep(
              icon: Icons.delivery_dining,
              label: 'Delivery',
            ),
          ),
          Icon(Icons.chevron_right),
          Expanded(
            child: _ProgressStep(
              icon: Icons.payments_outlined,
              label: 'Payment',
            ),
          ),
        ],
      ),
    ),
  );
}

class _ProgressStep extends StatelessWidget {
  const _ProgressStep({required this.icon, required this.label});
  final IconData icon;
  final String label;
  @override
  Widget build(BuildContext context) => Column(
    children: [
      Icon(icon),
      const SizedBox(height: 4),
      Text(
        label,
        textAlign: TextAlign.center,
        style: Theme.of(context).textTheme.labelMedium,
      ),
    ],
  );
}

class _ManageStopCard extends StatelessWidget {
  const _ManageStopCard({required this.stop, required this.busy});
  final DeliveryStop stop;
  final bool busy;
  @override
  Widget build(BuildContext context) {
    final cubit = context.read<DeliveryBatchCubit>();
    final canPickup = stop.status == DeliveryStopStatus.pending;
    final canDeliver = stop.status == DeliveryStopStatus.pickedUp;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              stop.restaurantName,
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 4),
            Text(stop.deliveryAddress),
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                FilledButton.tonal(
                  onPressed:
                      busy || !canPickup
                          ? null
                          : () => cubit.markPickup(stop.orderId),
                  child: const Text('Picked up'),
                ),
                FilledButton(
                  onPressed:
                      busy || !canDeliver
                          ? null
                          : () => cubit.markDelivered(stop.orderId),
                  child: const Text('Delivered'),
                ),
                OutlinedButton(
                  onPressed:
                      busy || stop.status == DeliveryStopStatus.delivered
                          ? null
                          : () => cubit.markFailed(stop.orderId),
                  child: const Text('Report failure'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
