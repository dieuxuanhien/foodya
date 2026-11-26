import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../domain/models/delivery_models.dart';
import '../../domain/repositories/delivery_repository.dart';
import '../cubit/delivery_batch_cubit.dart';
import '../cubit/delivery_batch_state.dart';
import '../widgets/delivery_prototype_widgets.dart';

class DeliveryBatchDetailPage extends StatelessWidget {
  const DeliveryBatchDetailPage({super.key, required this.batchId});
  final String batchId;

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create:
          (context) =>
              DeliveryBatchCubit(context.read<DeliveryRepository>())
                ..load(batchId),
      child: _BatchDetailView(batchId: batchId),
    );
  }
}

class _BatchDetailView extends StatelessWidget {
  const _BatchDetailView({required this.batchId});
  final String batchId;

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<DeliveryBatchCubit, DeliveryBatchState>(
      builder:
          (context, state) => Scaffold(
            appBar: AppBar(title: const Text('Batch Detail')),
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
    if (state.status == DeliveryBatchViewStatus.failure) {
      return DeliveryErrorCard(
        message: state.errorMessage ?? 'Unable to load batch.',
        onRetry: () => context.read<DeliveryBatchCubit>().load(batchId),
      );
    }
    final batch = state.batch;
    if (batch == null) {
      return const _BatchEmptyState();
    }
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  batch.batchId,
                  style: Theme.of(context).textTheme.titleLarge,
                ),
                const SizedBox(height: 8),
                Text(
                  '${batch.stops.length} stops · ${((batch.totalDistanceMeters ?? 0) / 1000).toStringAsFixed(1)} km · ${((batch.totalDurationSeconds ?? 0) / 60).round()} min',
                ),
                const SizedBox(height: 14),
                Container(
                  height: 150,
                  decoration: BoxDecoration(
                    color:
                        Theme.of(context).colorScheme.surfaceContainerHighest,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Center(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.map_outlined, size: 38),
                        SizedBox(height: 8),
                        Text('Route visualization placeholder'),
                        Text('Map routing is deferred to Phase 6'),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),
        Text('Ordered stops', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 8),
        ...batch.stops.map(
          (stop) => Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: _StopSummary(stop: stop),
          ),
        ),
        const SizedBox(height: 12),
        FilledButton.icon(
          onPressed:
              () => context.push('/delivery/batches/${batch.batchId}/manage'),
          icon: const Icon(Icons.route),
          label: const Text('Manage batch progress'),
        ),
        const SizedBox(height: 8),
        Text(
          'Batch-level accept/reject is unavailable in the verified backend contract.',
          textAlign: TextAlign.center,
          style: Theme.of(context).textTheme.bodySmall,
        ),
      ],
    );
  }
}

class _StopSummary extends StatelessWidget {
  const _StopSummary({required this.stop});
  final DeliveryStop stop;
  @override
  Widget build(BuildContext context) => Card(
    child: ListTile(
      leading: CircleAvatar(child: Text('${stop.pickupIndex}')),
      title: Text(stop.restaurantName),
      subtitle: Text(
        stop.deliveryAddress,
        maxLines: 2,
        overflow: TextOverflow.ellipsis,
      ),
      trailing: Chip(label: Text(_label(stop.status))),
    ),
  );

  String _label(DeliveryStopStatus status) => switch (status) {
    DeliveryStopStatus.pending => 'Pending',
    DeliveryStopStatus.pickedUp => 'Picked up',
    DeliveryStopStatus.delivering => 'Delivering',
    DeliveryStopStatus.delivered => 'Delivered',
    DeliveryStopStatus.failed => 'Failed',
    DeliveryStopStatus.unknown => 'Unknown',
  };
}

class _BatchEmptyState extends StatelessWidget {
  const _BatchEmptyState();
  @override
  Widget build(BuildContext context) => const Card(
    child: Padding(
      padding: EdgeInsets.all(28),
      child: Column(
        children: [
          Icon(Icons.route_outlined, size: 40),
          SizedBox(height: 12),
          Text('No active batch'),
          SizedBox(height: 4),
          Text(
            'The backend exposes one active batch rather than a general batch list.',
            textAlign: TextAlign.center,
          ),
        ],
      ),
    ),
  );
}
