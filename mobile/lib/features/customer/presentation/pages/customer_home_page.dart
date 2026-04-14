import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../auth/presentation/cubit/login_cubit.dart';

class CustomerHomePage extends StatelessWidget {
  const CustomerHomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Customer Home'),
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
            title: 'Discover Restaurants',
            subtitle: 'SRS FR07, FR08, FR09',
            icon: Icons.search,
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
