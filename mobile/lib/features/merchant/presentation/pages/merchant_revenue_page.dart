import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../domain/models/merchant_revenue_report.dart';
import '../../domain/repositories/merchant_revenue_repository.dart';
import '../cubit/merchant_revenue_cubit.dart';
import '../cubit/merchant_revenue_state.dart';

class MerchantRevenuePage extends StatelessWidget {
  const MerchantRevenuePage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create:
          (context) => MerchantRevenueCubit(
            repository: context.read<MerchantRevenueRepository>(),
          )..loadLast30Days(),
      child: const _MerchantRevenueView(),
    );
  }
}

class _MerchantRevenueView extends StatelessWidget {
  const _MerchantRevenueView();

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<MerchantRevenueCubit, MerchantRevenueState>(
      builder: (context, state) {
        final report = state.report;
        return Scaffold(
          appBar: AppBar(
            title: const Text('Revenue Dashboard'),
            actions: [
              IconButton(
                onPressed:
                    state.isLoading
                        ? null
                        : () =>
                            context
                                .read<MerchantRevenueCubit>()
                                .loadLast30Days(),
                icon: const Icon(Icons.refresh_outlined),
                tooltip: 'Refresh',
              ),
            ],
          ),
          body: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              if (state.isLoading && report == null)
                const Center(
                  child: Padding(
                    padding: EdgeInsets.symmetric(vertical: 24),
                    child: CircularProgressIndicator(),
                  ),
                )
              else if (state.errorMessage != null && report == null)
                Card(
                  child: ListTile(
                    leading: const Icon(Icons.error_outline),
                    title: const Text('Report unavailable'),
                    subtitle: Text(state.errorMessage!),
                  ),
                )
              else if (report != null) ...[
                _MetricGrid(report: report),
                const SizedBox(height: 16),
                _RevenueSeries(series: report.series),
                const SizedBox(height: 16),
                _TopSellingItems(items: report.topSellingItems),
              ],
            ],
          ),
        );
      },
    );
  }
}

class _MetricGrid extends StatelessWidget {
  const _MetricGrid({required this.report});

  final MerchantRevenueReport report;

  @override
  Widget build(BuildContext context) {
    return GridView.count(
      crossAxisCount: 2,
      childAspectRatio: 1.7,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      mainAxisSpacing: 12,
      crossAxisSpacing: 12,
      children: [
        _MetricCard(label: 'Revenue', value: _money(report.revenue)),
        _MetricCard(label: 'Profit', value: _money(report.platformProfit)),
        _MetricCard(label: 'Orders', value: report.orderCount.toString()),
        _MetricCard(label: 'Avg order', value: _money(report.avgOrderValue)),
      ],
    );
  }
}

class _MetricCard extends StatelessWidget {
  const _MetricCard({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(label, style: Theme.of(context).textTheme.labelMedium),
            const SizedBox(height: 8),
            Text(value, style: Theme.of(context).textTheme.titleMedium),
          ],
        ),
      ),
    );
  }
}

class _RevenueSeries extends StatelessWidget {
  const _RevenueSeries({required this.series});

  final List<MerchantRevenueBucket> series;

  @override
  Widget build(BuildContext context) {
    if (series.isEmpty) {
      return const Card(
        child: ListTile(
          leading: Icon(Icons.show_chart_outlined),
          title: Text('No revenue series'),
        ),
      );
    }
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text('Series', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 8),
        for (final bucket in series)
          Card(
            child: ListTile(
              leading: const Icon(Icons.show_chart_outlined),
              title: Text(_date(bucket.period)),
              subtitle: Text('${bucket.orderCount} orders'),
              trailing: Text(_money(bucket.revenue)),
            ),
          ),
      ],
    );
  }
}

class _TopSellingItems extends StatelessWidget {
  const _TopSellingItems({required this.items});

  final List<MerchantTopSellingItem> items;

  @override
  Widget build(BuildContext context) {
    if (items.isEmpty) {
      return const Card(
        child: ListTile(
          leading: Icon(Icons.leaderboard_outlined),
          title: Text('No top-selling items'),
        ),
      );
    }
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          'Top Selling Items',
          style: Theme.of(context).textTheme.titleMedium,
        ),
        const SizedBox(height: 8),
        for (final item in items)
          Card(
            child: ListTile(
              leading: const Icon(Icons.fastfood_outlined),
              title: Text(item.itemName),
              subtitle: Text('${item.quantitySold} sold'),
              trailing: Text(_money(item.revenue)),
            ),
          ),
      ],
    );
  }
}

String _money(double value) => value.toStringAsFixed(0);

String _date(DateTime value) {
  return value.toIso8601String().substring(0, 10);
}
