import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../auth/presentation/cubit/login_cubit.dart';

enum _MerchantSessionAction { refresh, logoutAll }

class MerchantHomePage extends StatelessWidget {
  const MerchantHomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Merchant Home'),
        actions: [
          PopupMenuButton<_MerchantSessionAction>(
            onSelected: (action) async {
              final cubit = context.read<LoginCubit>();
              switch (action) {
                case _MerchantSessionAction.refresh:
                  await cubit.refreshToken();
                  break;
                case _MerchantSessionAction.logoutAll:
                  await cubit.logoutAll();
                  break;
              }

              final state = cubit.state;
              final message = state.errorMessage ?? state.infoMessage;
              if (message != null && context.mounted) {
                ScaffoldMessenger.of(
                  context,
                ).showSnackBar(SnackBar(content: Text(message)));
                cubit.clearFeedback();
              }
            },
            itemBuilder:
                (context) => const [
                  PopupMenuItem(
                    value: _MerchantSessionAction.refresh,
                    child: Text('Refresh Token'),
                  ),
                  PopupMenuItem(
                    value: _MerchantSessionAction.logoutAll,
                    child: Text('Logout All Sessions'),
                  ),
                ],
            icon: const Icon(Icons.more_vert),
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: const [
          _FeatureCard(
            title: 'Restaurant and Menu Management',
            subtitle: 'SRS FR13',
            icon: Icons.restaurant_menu_outlined,
            routePath: '/merchant/restaurant',
          ),
          SizedBox(height: 12),
          _FeatureCard(
            title: 'Catalog Management',
            subtitle: 'SRS FR14, FR15',
            icon: Icons.fastfood_outlined,
            routePath: '/merchant/catalog',
          ),
          SizedBox(height: 12),
          _FeatureCard(
            title: 'Order Operations',
            subtitle: 'SRS FR16',
            icon: Icons.receipt_long_outlined,
            routePath: '/merchant/orders',
          ),
          SizedBox(height: 12),
          _FeatureCard(
            title: 'Review Center',
            subtitle: 'SRS FR16',
            icon: Icons.rate_review_outlined,
            routePath: '/merchant/reviews',
          ),
          SizedBox(height: 12),
          _FeatureCard(
            title: 'Revenue and Insights',
            subtitle: 'SRS FR25',
            icon: Icons.bar_chart_outlined,
            routePath: '/merchant/revenue',
          ),
          SizedBox(height: 12),
          _FeatureCard(
            title: 'Notifications',
            subtitle: 'SRS FR23',
            icon: Icons.notifications_active_outlined,
            routePath: '/merchant/notifications',
          ),
          SizedBox(height: 12),
          _FeatureCard(
            title: 'Profile and Password',
            subtitle: 'SRS FR04, FR05, FR06',
            icon: Icons.person_outline,
            routePath: '/merchant/profile',
          ),
        ],
      ),
    );
  }
}

class _FeatureCard extends StatelessWidget {
  const _FeatureCard({
    required this.title,
    required this.subtitle,
    required this.icon,
    this.routePath,
  });

  final String title;
  final String subtitle;
  final IconData icon;
  final String? routePath;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ListTile(
        contentPadding: const EdgeInsets.all(16),
        leading: Icon(icon),
        title: Text(title),
        subtitle: Text(subtitle),
        trailing:
            routePath == null
                ? null
                : const Icon(Icons.arrow_forward_ios_rounded, size: 16),
        onTap: routePath == null ? null : () => context.push(routePath!),
      ),
    );
  }
}
