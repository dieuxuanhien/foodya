import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/ui/foodya_ui.dart';
import '../../../auth/presentation/cubit/login_cubit.dart';
import '../../domain/models/merchant_order_summary.dart';
import '../../domain/models/merchant_restaurant.dart';
import '../../domain/repositories/merchant_order_repository.dart';
import '../../domain/repositories/merchant_restaurant_repository.dart';
import '../../domain/repositories/merchant_revenue_repository.dart';
import '../cubit/merchant_home_cubit.dart';
import '../cubit/merchant_home_state.dart';

enum _MerchantSessionAction { refresh, logoutAll }

class MerchantHomePage extends StatelessWidget {
  const MerchantHomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create:
          (context) => MerchantHomeCubit(
            restaurantRepository: context.read<MerchantRestaurantRepository>(),
            orderRepository: context.read<MerchantOrderRepository>(),
            revenueRepository: context.read<MerchantRevenueRepository>(),
          )..load(),
      child: const _MerchantHomeView(),
    );
  }
}

class _MerchantHomeView extends StatelessWidget {
  const _MerchantHomeView();

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<MerchantHomeCubit, MerchantHomeState>(
      builder: (context, state) {
        return Scaffold(
          appBar: AppBar(
            title: const Text('Dashboard'),
            actions: [
              IconButton(
                onPressed: () => context.push('/merchant/notifications'),
                icon: const Icon(Icons.notifications_outlined),
                tooltip: 'Alerts',
              ),
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

                  final loginState = cubit.state;
                  final message =
                      loginState.errorMessage ?? loginState.infoMessage;
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
          body: RefreshIndicator(
            onRefresh: () => context.read<MerchantHomeCubit>().load(),
            child: _MerchantHomeBody(state: state),
          ),
        );
      },
    );
  }
}

class _MerchantHomeBody extends StatelessWidget {
  const _MerchantHomeBody({required this.state});

  final MerchantHomeState state;

  @override
  Widget build(BuildContext context) {
    if (state.status == MerchantHomeStatus.loading ||
        state.status == MerchantHomeStatus.initial) {
      return ListView(
        padding: const EdgeInsets.all(16),
        children: const [
          SizedBox(
            height: 220,
            child: Center(child: CircularProgressIndicator()),
          ),
        ],
      );
    }

    if (state.status == MerchantHomeStatus.failure) {
      return ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _DashboardEmptyState(
            icon: Icons.wifi_off_outlined,
            title: 'Dashboard unavailable',
            message: state.errorMessage ?? 'Unable to load merchant dashboard.',
            actionLabel: 'Retry',
            onAction: () => context.read<MerchantHomeCubit>().load(),
          ),
        ],
      );
    }

    final restaurant = state.selectedRestaurant;
    if (restaurant == null) {
      return ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _DashboardEmptyState(
            icon: Icons.storefront_outlined,
            title: 'Create your restaurant',
            message: 'Set up a restaurant before taking orders.',
            actionLabel: 'Restaurant setup',
            onAction: () => context.push('/merchant/restaurant'),
          ),
        ],
      );
    }

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _RestaurantHero(restaurant: restaurant),
        const SizedBox(height: 18),
        GridView.count(
          crossAxisCount: 2,
          childAspectRatio: 1.35,
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          mainAxisSpacing: 10,
          crossAxisSpacing: 10,
          children: [
            FoodyaMetricTile(
              label: 'Pending',
              value: state.pendingOrderCount.toString(),
              icon: Icons.pending_actions_outlined,
            ),
            FoodyaMetricTile(
              label: 'Active',
              value: state.activeOrderCount.toString(),
              icon: Icons.room_service_outlined,
              accentColor: const Color(0xFF16A34A),
            ),
            FoodyaMetricTile(
              label: '7-day revenue',
              value: _money(state.sevenDayRevenue),
              icon: Icons.payments_outlined,
            ),
            FoodyaMetricTile(
              label: 'Rating',
              value: state.averageRating.toStringAsFixed(1),
              icon: Icons.star_outline,
              accentColor: const Color(0xFFF59E0B),
            ),
          ],
        ),
        const SizedBox(height: 20),
        FoodyaSectionHeader(
          title: 'Order queue',
          action: TextButton.icon(
            onPressed: () => context.push('/merchant/orders'),
            icon: const Icon(Icons.receipt_long_outlined),
            label: const Text('Orders'),
          ),
        ),
        const SizedBox(height: 10),
        _RecentOrderList(orders: state.orders.take(4).toList(growable: false)),
        const SizedBox(height: 20),
        const FoodyaSectionHeader(title: 'Quick actions'),
        const SizedBox(height: 12),
        GridView.count(
          crossAxisCount: 2,
          childAspectRatio: 2.7,
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          mainAxisSpacing: 10,
          crossAxisSpacing: 10,
          children: [
            FoodyaQuickActionTile(
              label: 'Catalog',
              icon: Icons.restaurant_menu_outlined,
              onTap: () => context.push('/merchant/catalog'),
            ),
            FoodyaQuickActionTile(
              label: 'Orders',
              icon: Icons.receipt_long_outlined,
              onTap: () => context.push('/merchant/orders'),
            ),
            FoodyaQuickActionTile(
              label: 'Insights',
              icon: Icons.bar_chart_outlined,
              onTap: () => context.push('/merchant/revenue'),
            ),
            FoodyaQuickActionTile(
              label: 'Reviews',
              icon: Icons.rate_review_outlined,
              onTap: () => context.push('/merchant/reviews'),
            ),
            FoodyaQuickActionTile(
              label: 'Alerts',
              icon: Icons.notifications_active_outlined,
              onTap: () => context.push('/merchant/notifications'),
            ),
          ],
        ),
      ],
    );
  }
}

class _RestaurantHero extends StatelessWidget {
  const _RestaurantHero({required this.restaurant});

  final MerchantRestaurant restaurant;

  @override
  Widget build(BuildContext context) {
    return FoodyaHomeHero(
      eyebrow: restaurant.open ? 'OPEN NOW' : 'CLOSED',
      title: restaurant.name,
      subtitle: '${restaurant.cuisineType} · ${restaurant.addressLine}',
      icon: Icons.storefront,
      trailing: FoodyaStatusChip(value: restaurant.status),
      primaryAction: FilledButton.icon(
        onPressed: () => context.push('/merchant/orders'),
        icon: const Icon(Icons.receipt_long_outlined),
        label: const Text('View orders'),
      ),
      secondaryAction: OutlinedButton.icon(
        style: OutlinedButton.styleFrom(
          foregroundColor: Colors.white,
          side: const BorderSide(color: Color(0xFFFFEDD5)),
        ),
        onPressed: () => context.push('/merchant/restaurant'),
        icon: const Icon(Icons.tune_outlined),
        label: const Text('Manage'),
      ),
    );
  }
}

class _RecentOrderList extends StatelessWidget {
  const _RecentOrderList({required this.orders});

  final List<MerchantOrderSummary> orders;

  @override
  Widget build(BuildContext context) {
    if (orders.isEmpty) {
      return const Card(
        child: Padding(
          padding: EdgeInsets.all(16),
          child: Text('No active orders right now.'),
        ),
      );
    }

    return Column(
      children: orders
          .map(
            (order) => Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: Card(
                child: ListTile(
                  onTap: () => context.push('/merchant/orders'),
                  leading: const CircleAvatar(
                    backgroundColor: Color(0xFFFFEDD5),
                    foregroundColor: Color(0xFFEA580C),
                    child: Icon(Icons.receipt_long_outlined),
                  ),
                  title: Text(
                    order.orderCode,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  subtitle: Text(
                    '${order.customerName} · ${_money(order.totalAmount)}',
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  trailing: FoodyaStatusChip(value: order.status),
                ),
              ),
            ),
          )
          .toList(growable: false),
    );
  }
}

class _DashboardEmptyState extends StatelessWidget {
  const _DashboardEmptyState({
    required this.icon,
    required this.title,
    required this.message,
    required this.actionLabel,
    required this.onAction,
  });

  final IconData icon;
  final String title;
  final String message;
  final String actionLabel;
  final VoidCallback onAction;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon, color: theme.colorScheme.primary, size: 32),
            const SizedBox(height: 12),
            Text(title, style: theme.textTheme.titleLarge),
            const SizedBox(height: 6),
            Text(
              message,
              style: theme.textTheme.bodyMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 16),
            FilledButton(onPressed: onAction, child: Text(actionLabel)),
          ],
        ),
      ),
    );
  }
}

String _money(double value) {
  if (value >= 1000000) {
    final compact = value / 1000000;
    return '${compact.toStringAsFixed(compact >= 10 ? 0 : 1)}M';
  }
  if (value >= 1000) {
    final compact = value / 1000;
    return '${compact.toStringAsFixed(compact >= 10 ? 0 : 1)}K';
  }
  return value.toStringAsFixed(0);
}
