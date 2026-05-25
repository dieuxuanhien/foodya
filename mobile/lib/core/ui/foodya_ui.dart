import 'package:flutter/material.dart';

class FoodyaSectionHeader extends StatelessWidget {
  const FoodyaSectionHeader({
    super.key,
    required this.title,
    this.subtitle,
    this.action,
  });

  final String title;
  final String? subtitle;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: Theme.of(context).textTheme.titleLarge),
              if (subtitle != null) ...[
                const SizedBox(height: 4),
                Text(
                  subtitle!,
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                  ),
                ),
              ],
            ],
          ),
        ),
        if (action != null) action!,
      ],
    );
  }
}

class FoodyaActionCard extends StatelessWidget {
  const FoodyaActionCard({
    super.key,
    required this.title,
    required this.subtitle,
    required this.icon,
    this.metric,
    this.onTap,
  });

  final String title;
  final String subtitle;
  final IconData icon;
  final String? metric;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  CircleAvatar(
                    backgroundColor: theme.colorScheme.primaryContainer,
                    foregroundColor: theme.colorScheme.onPrimaryContainer,
                    child: Icon(icon),
                  ),
                  const Spacer(),
                  if (metric != null)
                    Text(metric!, style: theme.textTheme.titleMedium),
                ],
              ),
              const SizedBox(height: 18),
              Text(title, style: theme.textTheme.titleMedium),
              const SizedBox(height: 4),
              Text(
                subtitle,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.bodySmall?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class FoodyaStatusChip extends StatelessWidget {
  const FoodyaStatusChip({super.key, required this.value, this.icon});

  final String value;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final label = friendlyStatusLabel(value);
    return Chip(
      avatar: icon == null ? null : Icon(icon, size: 16),
      label: Text(label),
      backgroundColor: statusColor(theme.colorScheme, value),
      labelStyle: TextStyle(color: statusTextColor(theme.colorScheme, value)),
    );
  }
}

String friendlyStatusLabel(String value) {
  return switch (value.toUpperCase()) {
    'PENDING' => 'Pending',
    'ACCEPTED' => 'Accepted',
    'ASSIGNED' => 'Assigned',
    'PREPARING' => 'Preparing',
    'DELIVERING' => 'On the way',
    'SUCCESS' => 'Completed',
    'CANCELLED' => 'Cancelled',
    'UNPAID' => 'Payment pending',
    'PAID' => 'Paid',
    'FAILED' => 'Failed',
    'REFUNDED' => 'Refunded',
    'OPEN' => 'Open',
    'CLOSED' => 'Closed',
    _ => value
        .replaceAll('_', ' ')
        .toLowerCase()
        .split(' ')
        .where((word) => word.isNotEmpty)
        .map((word) => '${word[0].toUpperCase()}${word.substring(1)}')
        .join(' '),
  };
}

Color statusColor(ColorScheme colors, String value) {
  return switch (value.toUpperCase()) {
    'SUCCESS' || 'PAID' || 'OPEN' => const Color(0xFFDCFCE7),
    'CANCELLED' || 'FAILED' || 'CLOSED' => colors.errorContainer,
    'DELIVERING' || 'ASSIGNED' => const Color(0xFFDBEAFE),
    _ => const Color(0xFFFFEDD5),
  };
}

Color statusTextColor(ColorScheme colors, String value) {
  return switch (value.toUpperCase()) {
    'SUCCESS' || 'PAID' || 'OPEN' => const Color(0xFF166534),
    'CANCELLED' || 'FAILED' || 'CLOSED' => colors.onErrorContainer,
    'DELIVERING' || 'ASSIGNED' => const Color(0xFF1E40AF),
    _ => const Color(0xFF9A3412),
  };
}
