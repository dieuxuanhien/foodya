import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/formatters/vnd_currency_formatter.dart';
import '../../domain/repositories/delivery_repository.dart';
import '../cubit/delivery_wallet_cubit.dart';
import '../cubit/delivery_wallet_state.dart';
import '../widgets/delivery_prototype_widgets.dart';

class DeliveryEarningsPage extends StatelessWidget {
  const DeliveryEarningsPage({super.key});

  @override
  Widget build(BuildContext context) => BlocProvider(
    create:
        (context) =>
            DeliveryWalletCubit(context.read<DeliveryRepository>())..load(),
    child: const _EarningsView(),
  );
}

class DeliveryWalletPage extends StatelessWidget {
  const DeliveryWalletPage({super.key});

  @override
  Widget build(BuildContext context) => const DeliveryEarningsPage();
}

class _EarningsView extends StatelessWidget {
  const _EarningsView();

  @override
  Widget build(BuildContext context) =>
      BlocBuilder<DeliveryWalletCubit, DeliveryWalletState>(
        builder:
            (context, state) => Scaffold(
              appBar: AppBar(title: const Text('Earnings Report')),
              body: DeliveryPrototypeBody(child: _content(context, state)),
            ),
      );

  Widget _content(BuildContext context, DeliveryWalletState state) {
    if (state.status == DeliveryWalletStatus.loading ||
        state.status == DeliveryWalletStatus.initial) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(48),
          child: CircularProgressIndicator(),
        ),
      );
    }
    if (state.status == DeliveryWalletStatus.failure) {
      return DeliveryErrorCard(
        message: state.errorMessage ?? 'Unable to load earnings.',
        onRetry: context.read<DeliveryWalletCubit>().load,
      );
    }
    final report = state.report!;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Delivery earnings · VND'),
                const SizedBox(height: 4),
                Text(
                  formatVndCurrency(report.totalEarnings),
                  style: Theme.of(context).textTheme.headlineMedium,
                ),
                const SizedBox(height: 8),
                Text('${report.deliveryCount} completed deliveries'),
                Text(
                  'COD cash collected: ${formatVndCurrency(report.codCashCollected)}',
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),
        Text(
          'Completed deliveries',
          style: Theme.of(context).textTheme.titleMedium,
        ),
        const SizedBox(height: 8),
        if (report.entries.isEmpty)
          const Card(
            child: Padding(
              padding: EdgeInsets.all(24),
              child: Text('No completed deliveries in this reporting period.'),
            ),
          )
        else
          for (final entry in report.entries)
            Card(
              child: ListTile(
                leading: const Icon(Icons.receipt_long_outlined),
                title: Text(entry.orderCode),
                subtitle: Text(
                  '${entry.paymentMethod} · ${entry.completedAt.toLocal().toString().split('.').first}',
                ),
                trailing: Text(formatVndCurrency(entry.driverEarning)),
              ),
            ),
        const SizedBox(height: 12),
        const Card(
          child: ListTile(
            leading: Icon(Icons.info_outline),
            title: Text('Read-only report'),
            subtitle: Text(
              'Transactions, withdrawals and settlement management are not available.',
            ),
          ),
        ),
        const SizedBox(height: 8),
        Text(
          'The current Delivery API omits currencyCode; Foodya is configured for VND.',
          textAlign: TextAlign.center,
          style: Theme.of(context).textTheme.bodySmall,
        ),
      ],
    );
  }
}
