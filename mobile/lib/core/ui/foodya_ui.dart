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

class FoodyaHomeHero extends StatelessWidget {
  const FoodyaHomeHero({
    super.key,
    required this.eyebrow,
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.primaryAction,
    this.secondaryAction,
    this.trailing,
  });

  final String eyebrow;
  final String title;
  final String subtitle;
  final IconData icon;
  final Widget primaryAction;
  final Widget? secondaryAction;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: const Color(0xFF431407),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              CircleAvatar(
                backgroundColor: Colors.white.withValues(alpha: 0.14),
                foregroundColor: Colors.white,
                child: Icon(icon),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      eyebrow,
                      style: theme.textTheme.labelMedium?.copyWith(
                        color: const Color(0xFFFED7AA),
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      title,
                      style: theme.textTheme.headlineSmall?.copyWith(
                        color: Colors.white,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                  ],
                ),
              ),
              if (trailing != null) ...[const SizedBox(width: 12), trailing!],
            ],
          ),
          const SizedBox(height: 10),
          Text(
            subtitle,
            style: theme.textTheme.bodyMedium?.copyWith(
              color: const Color(0xFFFFEDD5),
            ),
          ),
          const SizedBox(height: 16),
          LayoutBuilder(
            builder: (context, constraints) {
              final isTight = constraints.maxWidth < 360;
              final actions = [
                Expanded(child: primaryAction),
                if (secondaryAction != null) ...[
                  const SizedBox(width: 10),
                  Expanded(child: secondaryAction!),
                ],
              ];
              if (!isTight || secondaryAction == null) {
                return Row(children: actions);
              }
              return Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  primaryAction,
                  const SizedBox(height: 10),
                  secondaryAction!,
                ],
              );
            },
          ),
        ],
      ),
    );
  }
}

class FoodyaQuickActionTile extends StatelessWidget {
  const FoodyaQuickActionTile({
    super.key,
    required this.label,
    required this.icon,
    this.value,
    this.onTap,
  });

  final String label;
  final IconData icon;
  final String? value;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Row(
            children: [
              CircleAvatar(
                radius: 19,
                backgroundColor: const Color(0xFFFFEDD5),
                foregroundColor: theme.colorScheme.primary,
                child: Icon(icon, size: 21),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  label,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.titleSmall?.copyWith(
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
              if (value != null) ...[
                const SizedBox(width: 8),
                Text(value!, style: theme.textTheme.labelLarge),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class FoodyaMetricTile extends StatelessWidget {
  const FoodyaMetricTile({
    super.key,
    required this.label,
    required this.value,
    required this.icon,
    this.accentColor,
  });

  final String label;
  final String value;
  final IconData icon;
  final Color? accentColor;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final accent = accentColor ?? theme.colorScheme.primary;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon, color: accent, size: 22),
            const SizedBox(height: 12),
            Text(
              value,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.w800,
              ),
            ),
            const SizedBox(height: 2),
            Text(
              label,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class FoodyaImageSurface extends StatelessWidget {
  const FoodyaImageSurface({
    super.key,
    required this.imageUrl,
    required this.icon,
    this.height,
    this.width,
    this.borderRadius = 12,
  });

  final String? imageUrl;
  final IconData icon;
  final double? height;
  final double? width;
  final double borderRadius;

  @override
  Widget build(BuildContext context) {
    final normalizedUrl = imageUrl?.trim();
    final radius = BorderRadius.circular(borderRadius);
    final fallback = Container(
      height: height,
      width: width,
      decoration: BoxDecoration(
        color: const Color(0xFFFFEDD5),
        borderRadius: radius,
      ),
      child: Icon(icon, color: Theme.of(context).colorScheme.primary, size: 30),
    );

    if (normalizedUrl == null || normalizedUrl.isEmpty) {
      return fallback;
    }

    return ClipRRect(
      borderRadius: radius,
      child: Image.network(
        normalizedUrl,
        height: height,
        width: width,
        fit: BoxFit.cover,
        errorBuilder: (_, _, _) => fallback,
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
