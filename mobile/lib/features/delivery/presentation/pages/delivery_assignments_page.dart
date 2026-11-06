import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/formatters/vnd_currency_formatter.dart';
import '../../domain/models/delivery_models.dart';
import '../../domain/repositories/delivery_repository.dart';
import '../cubit/delivery_assignments_cubit.dart';
import '../cubit/delivery_assignments_state.dart';
import '../widgets/delivery_prototype_widgets.dart';

class DeliveryAssignmentsPage extends StatelessWidget {
  const DeliveryAssignmentsPage({super.key});

  @override
  Widget build(BuildContext context) => BlocProvider(
    create:
        (context) =>
            DeliveryAssignmentsCubit(context.read<DeliveryRepository>())
              ..load(),
    child: const _AssignmentsView(),
  );
}

class _AssignmentsView extends StatelessWidget {
  const _AssignmentsView();

  @override
  Widget build(BuildContext context) =>
      BlocConsumer<DeliveryAssignmentsCubit, DeliveryAssignmentsState>(
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
          context.read<DeliveryAssignmentsCubit>().clearFeedback();
        },
        builder:
            (context, state) => Scaffold(
              appBar: AppBar(
                title: const Text('Assigned Orders'),
                actions: [
                  IconButton(
                    tooltip: 'Refresh assigned orders',
                    onPressed:
                        state.pendingOrderId == null
                            ? () =>
                                context.read<DeliveryAssignmentsCubit>().load()
                            : null,
                    icon: const Icon(Icons.refresh),
                  ),
                ],
              ),
              body: DeliveryPrototypeBody(
                onRefresh:
                    () => context.read<DeliveryAssignmentsCubit>().load(
                      showLoading: false,
                    ),
                child: _content(context, state),
              ),
            ),
      );

  Widget _content(BuildContext context, DeliveryAssignmentsState state) {
    if (state.status == DeliveryAssignmentsStatus.initial ||
        state.status == DeliveryAssignmentsStatus.loading) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(48),
          child: CircularProgressIndicator(),
        ),
      );
    }
    if (state.status == DeliveryAssignmentsStatus.failure) {
      return DeliveryErrorCard(
        message: state.errorMessage ?? 'Unable to load assigned orders.',
        onRetry: context.read<DeliveryAssignmentsCubit>().load,
      );
    }
    if (state.assignments.isEmpty) {
      return const Card(
        child: Padding(
          padding: EdgeInsets.all(28),
          child: Column(
            children: [
              Icon(Icons.delivery_dining_outlined, size: 42),
              SizedBox(height: 12),
              Text('No assigned orders'),
              SizedBox(height: 4),
              Text('Pull down or use refresh to check again.'),
            ],
          ),
        ),
      );
    }
    return Column(
      children: [
        for (final assignment in state.assignments)
          Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: _AssignmentCard(
              assignment: assignment,
              pending: state.pendingOrderId == assignment.orderId,
            ),
          ),
      ],
    );
  }
}

class _AssignmentCard extends StatelessWidget {
  const _AssignmentCard({required this.assignment, required this.pending});
  final DeliveryAssignment assignment;
  final bool pending;

  bool get canRespond =>
      assignment.status == DeliveryOrderStatus.assigned ||
      assignment.status == DeliveryOrderStatus.preparing;

  @override
  Widget build(BuildContext context) => Card(
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
          Text('Customer: ${assignment.customerName}'),
          Text('Payment: ${assignment.paymentStatus}'),
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
              FilledButton.tonal(
                onPressed:
                    pending
                        ? null
                        : () async {
                          await context.push(
                            '/delivery/orders/${assignment.orderId}',
                            extra: assignment,
                          );
                          if (context.mounted) {
                            await context.read<DeliveryAssignmentsCubit>().load(
                              showLoading: false,
                            );
                          }
                        },
                child: const Text('Details'),
              ),
              FilledButton(
                onPressed:
                    !canRespond || pending
                        ? null
                        : () => context
                            .read<DeliveryAssignmentsCubit>()
                            .acknowledge(assignment.orderId),
                child: const Text('Acknowledge'),
              ),
              OutlinedButton(
                onPressed:
                    !canRespond || pending
                        ? null
                        : () => _confirmReject(context),
                child: const Text('Release'),
              ),
            ],
          ),
          if (pending) ...[
            const SizedBox(height: 12),
            const LinearProgressIndicator(),
          ],
        ],
      ),
    ),
  );

  Future<void> _confirmReject(BuildContext context) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder:
          (context) => AlertDialog(
            title: const Text('Release assignment?'),
            content: const Text(
              'This order will return to the assignment pool for another driver.',
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context, false),
                child: const Text('Cancel'),
              ),
              FilledButton(
                onPressed: () => Navigator.pop(context, true),
                child: const Text('Release'),
              ),
            ],
          ),
    );
    if (confirmed == true && context.mounted) {
      await context.read<DeliveryAssignmentsCubit>().reject(assignment.orderId);
    }
  }
}
