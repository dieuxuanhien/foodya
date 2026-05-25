import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/ui/foodya_ui.dart';
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
        children: [
          const _MerchantHero(),
          const SizedBox(height: 20),
          const FoodyaSectionHeader(
            title: 'Run your restaurant',
            subtitle: 'Manage orders, menu, customer feedback, and revenue.',
          ),
          const SizedBox(height: 12),
          GridView.count(
            crossAxisCount: 2,
            childAspectRatio: 0.95,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            mainAxisSpacing: 12,
            crossAxisSpacing: 12,
            children: [
              FoodyaActionCard(
                title: 'Restaurant',
                subtitle: 'Edit profile, hours, delivery range, and status.',
                icon: Icons.storefront_outlined,
                onTap: () => context.push('/merchant/restaurant'),
              ),
              FoodyaActionCard(
                title: 'Catalog',
                subtitle:
                    'Organize categories, items, prices, and availability.',
                icon: Icons.fastfood_outlined,
                onTap: () => context.push('/merchant/catalog'),
              ),
              FoodyaActionCard(
                title: 'Orders',
                subtitle: 'Accept incoming orders and update preparation.',
                icon: Icons.receipt_long_outlined,
                onTap: () => context.push('/merchant/orders'),
              ),
              FoodyaActionCard(
                title: 'Reviews',
                subtitle: 'Reply to customers and protect service quality.',
                icon: Icons.rate_review_outlined,
                onTap: () => context.push('/merchant/reviews'),
              ),
              FoodyaActionCard(
                title: 'Insights',
                subtitle: 'Track revenue, profit, and best-selling items.',
                icon: Icons.bar_chart_outlined,
                onTap: () => context.push('/merchant/revenue'),
              ),
              FoodyaActionCard(
                title: 'Alerts',
                subtitle: 'Read order, customer, and system updates.',
                icon: Icons.notifications_active_outlined,
                onTap: () => context.push('/merchant/notifications'),
              ),
            ],
          ),
          const SizedBox(height: 20),
          FoodyaActionCard(
            title: 'Merchant account',
            subtitle: 'Update contact details and password.',
            icon: Icons.person_outline,
            onTap: () => context.push('/merchant/profile'),
          ),
        ],
      ),
    );
  }
}

class _MerchantHero extends StatelessWidget {
  const _MerchantHero();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: theme.colorScheme.primaryContainer,
        borderRadius: BorderRadius.circular(24),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(
            Icons.restaurant_menu_outlined,
            color: theme.colorScheme.onPrimaryContainer,
            size: 32,
          ),
          const SizedBox(height: 12),
          Text(
            'Merchant dashboard',
            style: theme.textTheme.headlineSmall?.copyWith(
              fontWeight: FontWeight.w800,
              color: theme.colorScheme.onPrimaryContainer,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            'Keep the kitchen moving with clear order, catalog, and revenue tools.',
            style: theme.textTheme.bodyMedium?.copyWith(
              color: theme.colorScheme.onPrimaryContainer,
            ),
          ),
        ],
      ),
    );
  }
}
