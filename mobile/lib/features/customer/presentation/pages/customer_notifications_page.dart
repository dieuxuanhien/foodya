import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../domain/models/customer_notification.dart';
import '../../domain/repositories/customer_notification_repository.dart';
import '../cubit/notification_cubit.dart';
import '../cubit/notification_state.dart' as state_defs;

class CustomerNotificationsPage extends StatelessWidget {
  const CustomerNotificationsPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create:
          (context) => NotificationCubit(
            repository: context.read<CustomerNotificationRepository>(),
          )..load(),
      child: const _CustomerNotificationsView(),
    );
  }
}

class _CustomerNotificationsView extends StatelessWidget {
  const _CustomerNotificationsView();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Notifications')),
      body: BlocBuilder<NotificationCubit, state_defs.NotificationState>(
        builder: (context, state) {
          if (state.status == state_defs.NotificationStatus.loading) {
            return const Center(child: CircularProgressIndicator());
          }
          if (state.status == state_defs.NotificationStatus.failure) {
            return _CenteredMessage(
              message: state.errorMessage ?? 'Unable to load notifications.',
              onRetry: () => context.read<NotificationCubit>().load(),
            );
          }
          if (state.notifications.isEmpty) {
            return const _CenteredMessage(message: 'No notifications yet.');
          }

          return RefreshIndicator(
            onRefresh: () => context.read<NotificationCubit>().load(),
            child: ListView.separated(
              padding: const EdgeInsets.all(16),
              itemCount: state.notifications.length,
              separatorBuilder: (_, _) => const SizedBox(height: 8),
              itemBuilder:
                  (context, index) => _NotificationTile(
                    notification: state.notifications[index],
                  ),
            ),
          );
        },
      ),
    );
  }
}

class _NotificationTile extends StatelessWidget {
  const _NotificationTile({required this.notification});

  final CustomerNotification notification;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ListTile(
        leading: Icon(
          notification.isRead
              ? Icons.notifications_none_outlined
              : Icons.notifications_active_outlined,
        ),
        title: Text(notification.title),
        subtitle: Text(notification.message),
        trailing:
            notification.isRead
                ? null
                : IconButton(
                  tooltip: 'Mark as read',
                  icon: const Icon(Icons.done),
                  onPressed:
                      () => context.read<NotificationCubit>().markAsRead(
                        notification.id,
                      ),
                ),
      ),
    );
  }
}

class _CenteredMessage extends StatelessWidget {
  const _CenteredMessage({required this.message, this.onRetry});

  final String message;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(message, textAlign: TextAlign.center),
            if (onRetry != null) ...[
              const SizedBox(height: 12),
              FilledButton(onPressed: onRetry, child: const Text('Retry')),
            ],
          ],
        ),
      ),
    );
  }
}
