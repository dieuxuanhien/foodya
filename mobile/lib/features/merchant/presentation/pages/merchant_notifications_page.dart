import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../domain/models/merchant_notification.dart';
import '../../domain/repositories/merchant_notification_repository.dart';
import '../cubit/merchant_notifications_cubit.dart';
import '../cubit/merchant_notifications_state.dart';

class MerchantNotificationsPage extends StatelessWidget {
  const MerchantNotificationsPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create:
          (context) => MerchantNotificationsCubit(
            repository: context.read<MerchantNotificationRepository>(),
          )..load(),
      child: const _MerchantNotificationsView(),
    );
  }
}

class _MerchantNotificationsView extends StatelessWidget {
  const _MerchantNotificationsView();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Notifications')),
      body: BlocConsumer<
        MerchantNotificationsCubit,
        MerchantNotificationsState
      >(
        listenWhen:
            (previous, current) =>
                previous.errorMessage != current.errorMessage,
        listener: (context, state) {
          final message = state.errorMessage;
          if (message == null) {
            return;
          }
          ScaffoldMessenger.of(
            context,
          ).showSnackBar(SnackBar(content: Text(message)));
        },
        builder: (context, state) {
          if (state.status == MerchantNotificationsStatus.loading) {
            return const Center(child: CircularProgressIndicator());
          }
          if (state.status == MerchantNotificationsStatus.failure) {
            return _CenteredMessage(
              message: state.errorMessage ?? 'Unable to load notifications.',
              onRetry: () => context.read<MerchantNotificationsCubit>().load(),
            );
          }
          if (state.notifications.isEmpty) {
            return const _CenteredMessage(message: 'No notifications yet.');
          }

          final visibleNotifications = state.visibleNotifications;
          return RefreshIndicator(
            onRefresh: () => context.read<MerchantNotificationsCubit>().load(),
            child: ListView(
              padding: const EdgeInsets.all(16),
              children: [
                _NotificationFilters(
                  unreadCount: state.unreadCount,
                  showUnreadOnly: state.showUnreadOnly,
                ),
                const SizedBox(height: 12),
                if (visibleNotifications.isEmpty)
                  const _InlineEmptyState(message: 'No unread notifications.')
                else
                  ...visibleNotifications.map(
                    (notification) => Padding(
                      padding: const EdgeInsets.only(bottom: 8),
                      child: _NotificationTile(notification: notification),
                    ),
                  ),
              ],
            ),
          );
        },
      ),
    );
  }
}

class _NotificationFilters extends StatelessWidget {
  const _NotificationFilters({
    required this.unreadCount,
    required this.showUnreadOnly,
  });

  final int unreadCount;
  final bool showUnreadOnly;

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: [
          FilterChip(
            label: const Text('All'),
            selected: !showUnreadOnly,
            onSelected:
                (_) => context
                    .read<MerchantNotificationsCubit>()
                    .showUnreadOnly(false),
          ),
          const SizedBox(width: 8),
          FilterChip(
            label: Text('Unread ($unreadCount)'),
            selected: showUnreadOnly,
            onSelected:
                (_) => context
                    .read<MerchantNotificationsCubit>()
                    .showUnreadOnly(true),
          ),
        ],
      ),
    );
  }
}

class _NotificationTile extends StatelessWidget {
  const _NotificationTile({required this.notification});

  final MerchantNotification notification;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ListTile(
        contentPadding: const EdgeInsets.all(16),
        leading: Icon(
          notification.isRead
              ? Icons.notifications_none_outlined
              : Icons.notifications_active_outlined,
        ),
        title: Text(notification.title),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const SizedBox(height: 4),
            Text(notification.message),
            if (notification.sentAt != null) ...[
              const SizedBox(height: 8),
              Text(
                _formatTimestamp(notification.sentAt!),
                style: Theme.of(context).textTheme.bodySmall,
              ),
            ],
          ],
        ),
        trailing:
            notification.isRead
                ? null
                : IconButton(
                  tooltip: 'Mark as read',
                  icon: const Icon(Icons.done),
                  onPressed:
                      () => context
                          .read<MerchantNotificationsCubit>()
                          .markAsRead(notification.id),
                ),
        onTap:
            notification.isOrderNotification
                ? () => context.push('/merchant/orders')
                : null,
      ),
    );
  }

  String _formatTimestamp(DateTime value) {
    final local = value.toLocal();
    final day = local.day.toString().padLeft(2, '0');
    final month = local.month.toString().padLeft(2, '0');
    final hour = local.hour.toString().padLeft(2, '0');
    final minute = local.minute.toString().padLeft(2, '0');
    return '$day/$month/${local.year} $hour:$minute';
  }
}

class _InlineEmptyState extends StatelessWidget {
  const _InlineEmptyState({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 48),
      child: Text(message, textAlign: TextAlign.center),
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
