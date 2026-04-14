import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../auth/presentation/cubit/login_cubit.dart';

class MerchantHomePage extends StatelessWidget {
  const MerchantHomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Merchant Home'),
        actions: [
          IconButton(
            onPressed: () async {
              await context.read<LoginCubit>().logout();
            },
            icon: const Icon(Icons.logout),
            tooltip: 'Sign out',
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: const [
          _FeatureCard(
            title: 'Restaurant and Menu Management',
            subtitle: 'SRS FR13, FR14, FR15',
            icon: Icons.restaurant_menu_outlined,
          ),
          SizedBox(height: 12),
          _FeatureCard(
            title: 'Order Operations',
            subtitle: 'SRS FR16',
            icon: Icons.receipt_long_outlined,
          ),
          SizedBox(height: 12),
          _FeatureCard(
            title: 'Revenue and Insights',
            subtitle: 'SRS FR25',
            icon: Icons.bar_chart_outlined,
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
  });

  final String title;
  final String subtitle;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ListTile(
        contentPadding: const EdgeInsets.all(16),
        leading: Icon(icon),
        title: Text(title),
        subtitle: Text(subtitle),
      ),
    );
  }
}
