import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/formatters/vnd_currency_formatter.dart';
import '../../domain/repositories/delivery_repository.dart';
import '../cubit/delivery_wallet_cubit.dart';
import '../cubit/delivery_wallet_state.dart';
import '../widgets/delivery_prototype_widgets.dart';

class DeliveryWalletPage extends StatelessWidget {
  const DeliveryWalletPage({super.key});
  @override
  Widget build(BuildContext context) => BlocProvider(
    create:
        (context) =>
            DeliveryWalletCubit(context.read<DeliveryRepository>())..load(),
    child: const _WalletView(),
  );
}

class _WalletView extends StatelessWidget {
  const _WalletView();
  @override
  Widget build(BuildContext context) =>
      BlocBuilder<DeliveryWalletCubit, DeliveryWalletState>(
        builder:
            (context, state) => Scaffold(
              appBar: AppBar(title: const Text('Wallet Preview')),
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
        message: state.errorMessage ?? 'Unable to load earnings preview.',
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
                const Row(
                  children: [
                    CircleAvatar(child: Icon(Icons.delivery_dining)),
                    SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        'Nguyễn Quốc Hải',
                        style: TextStyle(fontWeight: FontWeight.w700),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 20),
                const Text('Illustrative earnings'),
                const SizedBox(height: 4),
                Text(
                  formatVndCurrency(report.totalEarnings),
                  style: Theme.of(context).textTheme.headlineMedium,
                ),
                const SizedBox(height: 8),
                Text(
                  '${report.deliveryCount} completed deliveries · COD held ${formatVndCurrency(report.codCashCollected)}',
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),
        const Card(
          child: Column(
            children: [
              ListTile(
                leading: Icon(Icons.receipt_long_outlined),
                title: Text('Earnings history'),
                subtitle: Text('Live integration planned for Phase 5'),
                trailing: Icon(Icons.lock_outline),
              ),
              Divider(height: 1),
              ListTile(
                leading: Icon(Icons.account_balance_outlined),
                title: Text('Settlement management'),
                subtitle: Text(
                  'Not available in the verified backend contract',
                ),
                trailing: Icon(Icons.block),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        Text(
          'This is not a transactional wallet. Values are deterministic demo fixtures.',
          textAlign: TextAlign.center,
          style: Theme.of(context).textTheme.bodySmall,
        ),
      ],
    );
  }
}
