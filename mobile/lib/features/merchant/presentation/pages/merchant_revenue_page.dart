import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/formatters/vnd_currency_formatter.dart';
import '../../../../core/theme/app_theme.dart';
import '../../../../core/ui/foodya_ui.dart';
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
    return BlocConsumer<MerchantRevenueCubit, MerchantRevenueState>(
      listenWhen:
          (previous, current) => previous.errorMessage != current.errorMessage,
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
        final report = state.report;
        return Scaffold(
          appBar: AppBar(
            title: const Text('Revenue Dashboard'),
            actions: [
              IconButton(
                onPressed:
                    report == null
                        ? null
                        : () => _copyReportSummary(context, report),
                icon: const Icon(Icons.ios_share_outlined),
                tooltip: 'Export summary',
              ),
              IconButton(
                onPressed:
                    state.isLoading
                        ? null
                        : () => context.read<MerchantRevenueCubit>().load(
                          from: state.from,
                          to: state.to,
                          topItems: state.topItems,
                        ),
                icon: const Icon(Icons.refresh_outlined),
                tooltip: 'Refresh',
              ),
            ],
          ),
          body: RefreshIndicator(
            onRefresh:
                () => context.read<MerchantRevenueCubit>().load(
                  from: state.from,
                  to: state.to,
                  topItems: state.topItems,
                ),
            child: ListView(
              padding: const EdgeInsets.all(16),
              children: [
                _ReportControls(state: state),
                const SizedBox(height: 16),
                if (state.isLoading && report == null)
                  const Center(
                    child: Padding(
                      padding: EdgeInsets.symmetric(vertical: 24),
                      child: CircularProgressIndicator(),
                    ),
                  )
                else if (state.errorMessage != null && report == null)
                  FoodyaEmptyState(
                    illustrationAsset:
                        'assets/illustrations/empty_connection.png',
                    icon: Icons.error_outline,
                    title: 'Report unavailable',
                    message: state.errorMessage!,
                    actionLabel: 'Retry',
                    onAction:
                        () => context.read<MerchantRevenueCubit>().load(
                          from: state.from,
                          to: state.to,
                          topItems: state.topItems,
                        ),
                  )
                else if (report != null) ...[
                  _ReportPeriod(report: report),
                  const SizedBox(height: 12),
                  _MetricGrid(report: report),
                  const SizedBox(height: 16),
                  _RevenueChart(series: report.series),
                  const SizedBox(height: 16),
                  _RevenueSeries(series: report.series),
                  const SizedBox(height: 16),
                  _TopSellingItems(items: report.topSellingItems),
                ],
              ],
            ),
          ),
        );
      },
    );
  }

  Future<void> _copyReportSummary(
    BuildContext context,
    MerchantRevenueReport report,
  ) async {
    final summary = [
      'Foodya Merchant Revenue Report',
      'Period: ${_date(report.fromDate)} - ${_date(report.toDate)}',
      'Revenue: ${formatVndCurrency(report.revenue)}',
      'Platform profit: ${formatVndCurrency(report.platformProfit)}',
      'Orders: ${report.orderCount}',
      'Average order: ${formatVndCurrency(report.avgOrderValue)}',
      if (report.topSellingItems.isNotEmpty) 'Top items:',
      for (final item in report.topSellingItems)
        '- ${item.itemName}: ${item.quantitySold} sold, ${formatVndCurrency(item.revenue)}',
    ].join('\n');
    await Clipboard.setData(ClipboardData(text: summary));
    if (context.mounted) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Report summary copied.')));
    }
  }
}

class _ReportControls extends StatelessWidget {
  const _ReportControls({required this.state});

  final MerchantRevenueState state;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: Row(
            children: [
              ChoiceChip(
                label: const Text('7 days'),
                selected: _isPreset(days: 7),
                onSelected:
                    state.isLoading
                        ? null
                        : (_) =>
                            context
                                .read<MerchantRevenueCubit>()
                                .loadLast7Days(),
              ),
              const SizedBox(width: 8),
              ChoiceChip(
                label: const Text('30 days'),
                selected: _isPreset(days: 30),
                onSelected:
                    state.isLoading
                        ? null
                        : (_) =>
                            context
                                .read<MerchantRevenueCubit>()
                                .loadLast30Days(),
              ),
              const SizedBox(width: 8),
              ChoiceChip(
                label: const Text('90 days'),
                selected: _isPreset(days: 90),
                onSelected:
                    state.isLoading
                        ? null
                        : (_) =>
                            context
                                .read<MerchantRevenueCubit>()
                                .loadLast90Days(),
              ),
              const SizedBox(width: 8),
              ActionChip(
                avatar: const Icon(Icons.date_range_outlined),
                label: Text(_rangeLabel),
                onPressed:
                    state.isLoading ? null : () => _pickDateRange(context),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            const Icon(Icons.leaderboard_outlined, size: 20),
            const SizedBox(width: 8),
            const Expanded(child: Text('Top-selling items')),
            DropdownButton<int>(
              value: state.topItems,
              items: const [3, 5, 10, 20]
                  .map(
                    (value) => DropdownMenuItem<int>(
                      value: value,
                      child: Text(value.toString()),
                    ),
                  )
                  .toList(growable: false),
              onChanged:
                  state.isLoading
                      ? null
                      : (value) {
                        if (value == null) {
                          return;
                        }
                        context.read<MerchantRevenueCubit>().setTopItems(value);
                      },
            ),
          ],
        ),
      ],
    );
  }

  bool _isPreset({required int days}) {
    final from = state.from;
    final to = state.to;
    if (from == null || to == null) {
      return false;
    }
    final difference = _dateOnly(to).difference(_dateOnly(from)).inDays;
    return difference == days;
  }

  String get _rangeLabel {
    final from = state.from;
    final to = state.to;
    if (from == null || to == null) {
      return 'Custom range';
    }
    return '${_shortDate(from)} - ${_shortDate(to)}';
  }

  Future<void> _pickDateRange(BuildContext context) async {
    final now = DateTime.now();
    final currentFrom = state.from ?? now.subtract(const Duration(days: 30));
    final currentTo = state.to ?? now;
    final range = await showDateRangePicker(
      context: context,
      firstDate: DateTime(now.year - 2),
      lastDate: now,
      initialDateRange: DateTimeRange(start: currentFrom, end: currentTo),
    );
    if (range == null || !context.mounted) {
      return;
    }
    await context.read<MerchantRevenueCubit>().setDateRange(
      from: range.start,
      to: range.end,
    );
  }
}

class _ReportPeriod extends StatelessWidget {
  const _ReportPeriod({required this.report});

  final MerchantRevenueReport report;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        const Icon(Icons.calendar_month_outlined, size: 20),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            '${_date(report.fromDate)} to ${_date(report.toDate)}',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
        ),
      ],
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
        FoodyaMetricTile(
          label: 'Revenue',
          value: formatVndCurrency(report.revenue),
          icon: Icons.payments_outlined,
          accentColor: const Color(0xFFEA580C),
        ),
        FoodyaMetricTile(
          label: 'Profit',
          value: formatVndCurrency(report.platformProfit),
          icon: Icons.savings_outlined,
          accentColor: const Color(0xFF16A34A),
        ),
        FoodyaMetricTile(
          label: 'Orders',
          value: report.orderCount.toString(),
          icon: Icons.receipt_long_outlined,
          accentColor: const Color(0xFFD97706),
        ),
        FoodyaMetricTile(
          label: 'Avg order',
          value: formatVndCurrency(report.avgOrderValue),
          icon: Icons.calculate_outlined,
          accentColor: const Color(0xFFF59E0B),
        ),
      ],
    );
  }
}

class _RevenueChart extends StatelessWidget {
  const _RevenueChart({required this.series});

  final List<MerchantRevenueBucket> series;

  @override
  Widget build(BuildContext context) {
    if (series.isEmpty) {
      return const FoodyaEmptyState(
        icon: Icons.bar_chart_outlined,
        title: 'No chart data',
        message: 'Revenue trends will appear here once orders come in.',
      );
    }

    final visible =
        series.length <= 12 ? series : series.sublist(series.length - 12);
    final maxRevenue = visible
        .map((bucket) => bucket.revenue)
        .fold<double>(0, (max, value) => value > max ? value : max);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text('Revenue Trend', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 8),
        SizedBox(
          height: 180,
          child: DecoratedBox(
            decoration: BoxDecoration(
              border: Border.all(color: Theme.of(context).dividerColor),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Padding(
              padding: const EdgeInsets.fromLTRB(12, 16, 12, 8),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  for (final bucket in visible)
                    Expanded(
                      child: Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 3),
                        child: _ChartBar(
                          bucket: bucket,
                          heightFactor:
                              maxRevenue <= 0 ? 0 : bucket.revenue / maxRevenue,
                        ),
                      ),
                    ),
                ],
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _ChartBar extends StatelessWidget {
  const _ChartBar({required this.bucket, required this.heightFactor});

  final MerchantRevenueBucket bucket;
  final double heightFactor;

  @override
  Widget build(BuildContext context) {
    return Tooltip(
      message:
          '${_date(bucket.period)}\n${formatVndCurrency(bucket.revenue)}\n${bucket.orderCount} orders',
      child: Column(
        mainAxisAlignment: MainAxisAlignment.end,
        children: [
          Expanded(
            child: Align(
              alignment: Alignment.bottomCenter,
              child: FractionallySizedBox(
                heightFactor: heightFactor.clamp(0.06, 1),
                widthFactor: 1,
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    gradient: AppTheme.accentGradient,
                    borderRadius: BorderRadius.circular(4),
                  ),
                ),
              ),
            ),
          ),
          const SizedBox(height: 6),
          Text(
            '${bucket.period.month}/${bucket.period.day}',
            maxLines: 1,
            overflow: TextOverflow.fade,
            softWrap: false,
            style: Theme.of(context).textTheme.labelSmall,
          ),
        ],
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
      return const FoodyaEmptyState(
        icon: Icons.show_chart_outlined,
        title: 'No revenue series',
        message: 'Daily revenue breakdowns will show up here once available.',
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
              subtitle: Text(
                '${bucket.orderCount} orders | Avg ${formatVndCurrency(bucket.avgOrderValue)}',
              ),
              trailing: Text(formatVndCurrency(bucket.revenue)),
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
      return const FoodyaEmptyState(
        icon: Icons.leaderboard_outlined,
        title: 'No top-selling items',
        message: 'Best sellers will be ranked here once orders roll in.',
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
              trailing: Text(formatVndCurrency(item.revenue)),
            ),
          ),
      ],
    );
  }
}

String _date(DateTime value) {
  return value.toIso8601String().substring(0, 10);
}

String _shortDate(DateTime value) {
  final day = value.day.toString().padLeft(2, '0');
  final month = value.month.toString().padLeft(2, '0');
  return '$day/$month';
}

DateTime _dateOnly(DateTime value) {
  return DateTime(value.year, value.month, value.day);
}
