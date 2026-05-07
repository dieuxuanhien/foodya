import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../auth/presentation/cubit/login_cubit.dart';

enum _CustomerSessionAction { refresh, logoutAll }

class CustomerHomePage extends StatelessWidget {
  const CustomerHomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Customer Home'),
        actions: [
          PopupMenuButton<_CustomerSessionAction>(
            onSelected: (action) async {
              final cubit = context.read<LoginCubit>();
              switch (action) {
                case _CustomerSessionAction.refresh:
                  await cubit.refreshToken();
                  break;
                case _CustomerSessionAction.logoutAll:
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
                    value: _CustomerSessionAction.refresh,
                    child: Text('Refresh Token'),
                  ),
                  PopupMenuItem(
                    value: _CustomerSessionAction.logoutAll,
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
            title: 'Discover Restaurants',
            subtitle: 'SRS FR07, FR08, FR09',
            icon: Icons.search,
            routePath: '/customer/restaurants',
          ),
          SizedBox(height: 12),
          _FeatureCard(
            title: 'Manage Cart and Checkout',
            subtitle: 'SRS FR10, FR27',
            icon: Icons.shopping_bag_outlined,
          ),
          SizedBox(height: 12),
          _FeatureCard(
            title: 'Track Orders and Reviews',
            subtitle: 'SRS FR11, FR12',
            icon: Icons.delivery_dining_outlined,
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
