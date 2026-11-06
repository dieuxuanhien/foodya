import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../auth/presentation/cubit/login_cubit.dart';
import '../../domain/models/delivery_models.dart';
import '../../domain/repositories/delivery_repository.dart';
import '../../domain/services/delivery_position_provider.dart';
import '../cubit/delivery_home_cubit.dart';
import '../cubit/delivery_home_state.dart';
import '../widgets/delivery_prototype_widgets.dart';

class DeliveryHomePage extends StatelessWidget {
  const DeliveryHomePage({super.key});

  @override
  Widget build(BuildContext context) => BlocProvider(
    create:
        (context) => DeliveryHomeCubit(
          context.read<DeliveryRepository>(),
          context.read<DeliveryPositionProvider>(),
        )..load(),
    child: const _DeliveryHomeView(),
  );
}

class _DeliveryHomeView extends StatelessWidget {
  const _DeliveryHomeView();

  @override
  Widget build(BuildContext context) =>
      BlocConsumer<DeliveryHomeCubit, DeliveryHomeState>(
        listenWhen:
            (previous, current) =>
                previous.infoMessage != current.infoMessage ||
                previous.errorMessage != current.errorMessage,
        listener: (context, state) {
          final message = state.errorMessage ?? state.infoMessage;
          if (message == null) return;
          ScaffoldMessenger.of(context)
            ..hideCurrentSnackBar()
            ..showSnackBar(SnackBar(content: Text(message)));
          context.read<DeliveryHomeCubit>().clearFeedback();
        },
        builder:
            (context, state) => Scaffold(
              appBar: AppBar(
                title: const Text('Delivery Home'),
                actions: [
                  IconButton(
                    tooltip: 'Log out all sessions',
                    onPressed:
                        state.isBusy
                            ? null
                            : context.read<LoginCubit>().logoutAll,
                    icon: const Icon(Icons.logout),
                  ),
                ],
              ),
              body: DeliveryPrototypeBody(child: _content(context, state)),
            ),
      );

  Widget _content(BuildContext context, DeliveryHomeState state) {
    if (state.status == DeliveryHomeStatus.initial ||
        state.status == DeliveryHomeStatus.loading) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(48),
          child: CircularProgressIndicator(),
        ),
      );
    }
    if (state.status == DeliveryHomeStatus.failure && state.session == null) {
      return DeliveryErrorCard(
        message: state.errorMessage ?? 'Unable to load Delivery status.',
        onRetry: context.read<DeliveryHomeCubit>().load,
      );
    }
    final session = state.session!;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _SessionCard(session: session, busy: state.isBusy),
        const SizedBox(height: 16),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(
                  'Driver actions',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const SizedBox(height: 12),
                FilledButton.icon(
                  onPressed:
                      state.isBusy || !session.canToggleAvailability
                          ? null
                          : context
                              .read<DeliveryHomeCubit>()
                              .toggleAvailability,
                  icon: Icon(
                    session.isOnline ? Icons.stop_circle : Icons.play_circle,
                  ),
                  label: Text(session.isOnline ? 'Go offline' : 'Go online'),
                ),
                const SizedBox(height: 8),
                OutlinedButton.icon(
                  onPressed:
                      state.isBusy || !session.isOnline
                          ? null
                          : context.read<DeliveryHomeCubit>().refreshLocation,
                  icon: const Icon(Icons.my_location),
                  label: const Text('Update current location'),
                ),
                const SizedBox(height: 8),
                OutlinedButton.icon(
                  onPressed: () => context.go('/delivery/orders'),
                  icon: const Icon(Icons.receipt_long_outlined),
                  label: const Text('View assigned orders'),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class _SessionCard extends StatelessWidget {
  const _SessionCard({required this.session, required this.busy});
  final DeliverySession session;
  final bool busy;

  @override
  Widget build(BuildContext context) => Card(
    child: Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const CircleAvatar(
                radius: 28,
                child: Icon(Icons.delivery_dining, size: 30),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Delivery driver',
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                    Text(session.status.label),
                  ],
                ),
              ),
              if (busy)
                const SizedBox.square(
                  dimension: 22,
                  child: CircularProgressIndicator(strokeWidth: 2),
                ),
            ],
          ),
          const SizedBox(height: 20),
          Text(
            '${session.activeOrderCount}/${session.maxConcurrentOrders} active orders',
          ),
          const SizedBox(height: 8),
          Text(
            session.currentLat == null || session.currentLng == null
                ? 'Location not submitted'
                : 'Location ${session.currentLat!.toStringAsFixed(5)}, ${session.currentLng!.toStringAsFixed(5)}',
          ),
          if (session.status == DriverAvailability.goingOffline) ...[
            const SizedBox(height: 12),
            const Text(
              'Last trip mode: you will go offline after the current delivery.',
            ),
          ],
          if (session.status == DriverAvailability.suspended) ...[
            const SizedBox(height: 12),
            Text(
              'This driver account is suspended.',
              style: TextStyle(color: Theme.of(context).colorScheme.error),
            ),
          ],
        ],
      ),
    ),
  );
}
