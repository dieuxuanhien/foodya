import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../domain/repositories/delivery_repository.dart';

class DeliveryPrototypeBody extends StatelessWidget {
  const DeliveryPrototypeBody({super.key, required this.child});
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final isDemo = context.read<DeliveryRepository>().isDemo;
    return Align(
      alignment: Alignment.topCenter,
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 880),
        child: ListView(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
          children: [
            if (isDemo) ...[
              const DeliveryDemoBanner(),
              const SizedBox(height: 12),
            ],
            child,
          ],
        ),
      ),
    );
  }
}

class DeliveryDemoBanner extends StatelessWidget {
  const DeliveryDemoBanner({super.key});

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label:
          'Demo Data. Values shown are illustrative and not from a live backend.',
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 9),
        decoration: BoxDecoration(
          color: Theme.of(context).colorScheme.tertiaryContainer,
          borderRadius: BorderRadius.circular(12),
        ),
        child: const Row(
          children: [
            Icon(Icons.science_outlined, size: 20),
            SizedBox(width: 8),
            Expanded(
              child: Text(
                'Demo Data · Interface prototype only',
                style: TextStyle(fontWeight: FontWeight.w700),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class DeliveryErrorCard extends StatelessWidget {
  const DeliveryErrorCard({
    super.key,
    required this.message,
    required this.onRetry,
  });
  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          children: [
            Icon(
              Icons.cloud_off_outlined,
              size: 40,
              color: Theme.of(context).colorScheme.error,
            ),
            const SizedBox(height: 12),
            Text(message, textAlign: TextAlign.center),
            const SizedBox(height: 16),
            FilledButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh),
              label: const Text('Retry'),
            ),
          ],
        ),
      ),
    );
  }
}
