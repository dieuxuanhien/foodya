import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/formatters/vnd_currency_formatter.dart';
import '../../../auth/presentation/cubit/login_cubit.dart';
import '../../domain/models/delivery_models.dart';
import '../../domain/repositories/delivery_repository.dart';
import '../cubit/delivery_home_cubit.dart';
import '../cubit/delivery_home_state.dart';
import '../widgets/delivery_prototype_widgets.dart';

class DeliveryHomePage extends StatelessWidget {
  const DeliveryHomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create:
          (context) =>
              DeliveryHomeCubit(context.read<DeliveryRepository>())..load(),
      child: const _DeliveryHomeView(),
    );
  }
}

class _DeliveryHomeView extends StatelessWidget {
  const _DeliveryHomeView();

  @override
  Widget build(BuildContext context) {
    return BlocConsumer<DeliveryHomeCubit, DeliveryHomeState>(
      listenWhen:
          (previous, current) =>
              previous.errorMessage != current.errorMessage ||
              previous.infoMessage != current.infoMessage,
      listener: (context, state) {
        final message = state.errorMessage ?? state.infoMessage;
        if (message == null) return;
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(message)));
        context.read<DeliveryHomeCubit>().clearFeedback();
      },
      builder: (context, state) {
        return Scaffold(
          appBar: AppBar(
            title: const Text('Delivery Home'),
            actions: [
              Semantics(
                button: true,
                label: 'Log out',
                child: IconButton(
                  tooltip: 'Log out',
                  onPressed:
                      state.isBusy
                          ? null
                          : () => context.read<LoginCubit>().logoutAll(),
                  icon: const Icon(Icons.logout),
                ),
              ),
            ],
          ),
          body: DeliveryPrototypeBody(child: _content(context, state)),
        );
      },
    );
  }

  Widget _content(BuildContext context, DeliveryHomeState state) {
    if (state.status == DeliveryHomeStatus.loading ||
        state.status == DeliveryHomeStatus.initial) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(48),
          child: CircularProgressIndicator(),
        ),
      );
    }
    if (state.status == DeliveryHomeStatus.failure && state.session == null) {
      return DeliveryErrorCard(
        message: state.errorMessage ?? 'Unable to load Delivery prototype.',
        onRetry: context.read<DeliveryHomeCubit>().load,
      );
    }
    final session = state.session;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _DriverHeader(
          session: session,
          busy: state.isBusy,
          onToggle: context.read<DeliveryHomeCubit>().toggleAvailability,
        ),
        const SizedBox(height: 20),
        Text('Batch', style: Theme.of(context).textTheme.titleLarge),
        const SizedBox(height: 8),
        if (state.activeBatch case final batch?)
          _BatchCard(batch: batch)
        else
          const _EmptyCard(
            icon: Icons.route_outlined,
            message: 'No active batch in this scenario.',
          ),
        const SizedBox(height: 20),
        Text('Single orders', style: Theme.of(context).textTheme.titleLarge),
        const SizedBox(height: 8),
        if (state.assignments.isEmpty)
          const _EmptyCard(
            icon: Icons.delivery_dining_outlined,
            message: 'No assigned orders right now.',
          )
        else
          ...state.assignments.map(
            (assignment) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: _AssignmentCard(
                assignment: assignment,
                busy: state.isBusy,
                onAccept:
                    () => context.read<DeliveryHomeCubit>().accept(
                      assignment.orderId,
                    ),
                onReject:
                    () => context.read<DeliveryHomeCubit>().reject(
                      assignment.orderId,
                    ),
              ),
            ),
          ),
      ],
    );
  }
}

class _DriverHeader extends StatelessWidget {
  const _DriverHeader({
    required this.session,
    required this.busy,
    required this.onToggle,
  });
  final DeliverySession? session;
  final bool busy;
  final VoidCallback onToggle;

  @override
  Widget build(BuildContext context) {
    final current = session;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Wrap(
          runSpacing: 16,
          spacing: 16,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            const CircleAvatar(
              radius: 28,
              child: Icon(Icons.delivery_dining, size: 30),
            ),
            SizedBox(
              width: 260,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Nguyễn Quốc Hải',
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  const SizedBox(height: 4),
                  Text(
                    current == null
                        ? 'Unavailable'
                        : '${current.status.label} · ${current.activeOrderCount}/${current.maxConcurrentOrders} active',
                  ),
                  if (current?.message case final message?)
                    Text(message, style: Theme.of(context).textTheme.bodySmall),
                ],
              ),
            ),
            FilledButton.icon(
              onPressed: current == null || busy ? null : onToggle,
              icon: Icon(
                current?.isOnline == true
                    ? Icons.stop_circle_outlined
                    : Icons.play_circle_outline,
              ),
              label: Text(
                current?.isOnline == true ? 'Go offline' : 'Go online',
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _BatchCard extends StatelessWidget {
  const _BatchCard({required this.batch});
  final DeliveryBatch batch;
  @override
  Widget build(BuildContext context) {
    return Card(
      child: ListTile(
        minVerticalPadding: 16,
        leading: const Icon(Icons.alt_route, size: 32),
        title: Text(batch.batchId),
        subtitle: Text(
          '${batch.stops.length} stops · ${((batch.totalDistanceMeters ?? 0) / 1000).toStringAsFixed(1)} km',
        ),
        trailing: SizedBox(
          width: 112,
          child: FilledButton.tonal(
            onPressed: () => context.push('/delivery/batches/${batch.batchId}'),
            child: const Text('View detail'),
          ),
        ),
      ),
    );
  }
}

class _AssignmentCard extends StatelessWidget {
  const _AssignmentCard({
    required this.assignment,
    required this.busy,
    required this.onAccept,
    required this.onReject,
  });
  final DeliveryAssignment assignment;
  final bool busy;
  final VoidCallback onAccept;
  final VoidCallback onReject;
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
                    assignment.orderCode,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),
                Chip(label: Text(assignment.status.label)),
              ],
            ),
            Text(assignment.restaurantName),
            Text('To ${assignment.customerName}'),
            if (assignment.deliveryAddress case final address?)
              Padding(
                padding: const EdgeInsets.only(top: 6),
                child: Text(
                  address,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            const SizedBox(height: 8),
            Text(
              formatVndCurrency(assignment.totalAmount),
              style: Theme.of(context).textTheme.titleSmall,
            ),
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                FilledButton(
                  onPressed: busy ? null : onAccept,
                  child: const Text('Acknowledge'),
                ),
                OutlinedButton(
                  onPressed: busy ? null : onReject,
                  child: const Text('Release'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _EmptyCard extends StatelessWidget {
  const _EmptyCard({required this.icon, required this.message});
  final IconData icon;
  final String message;
  @override
  Widget build(BuildContext context) => Card(
    child: Padding(
      padding: const EdgeInsets.all(24),
      child: Row(
        children: [
          Icon(icon),
          const SizedBox(width: 12),
          Expanded(child: Text(message)),
        ],
      ),
    ),
  );
}
