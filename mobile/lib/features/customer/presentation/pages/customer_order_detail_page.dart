import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/ui/foodya_ui.dart';
import '../../../../core/realtime/order_tracking_realtime_service.dart';
import '../../domain/models/order_detail.dart';
import '../../domain/models/order_tracking_point.dart';
import '../../domain/repositories/customer_order_repository.dart';
import '../cubit/order_detail_cubit.dart';
import '../cubit/order_detail_state.dart';

class CustomerOrderDetailPage extends StatelessWidget {
  const CustomerOrderDetailPage({super.key, required this.orderId});

  final String orderId;

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create:
          (context) => OrderDetailCubit(
            repository: context.read<CustomerOrderRepository>(),
            realtimeService: context.read<OrderTrackingRealtimeService>(),
          )..load(orderId),
      child: const _CustomerOrderDetailView(),
    );
  }
}

class _CustomerOrderDetailView extends StatelessWidget {
  const _CustomerOrderDetailView();

  @override
  Widget build(BuildContext context) {
    return BlocConsumer<OrderDetailCubit, OrderDetailState>(
      listenWhen:
          (previous, current) =>
              previous.errorMessage != current.errorMessage ||
              previous.infoMessage != current.infoMessage,
      listener: (context, state) {
        final message = state.errorMessage ?? state.infoMessage;
        if (message == null) {
          return;
        }
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(message)));
        context.read<OrderDetailCubit>().clearFeedback();
      },
      builder: (context, state) {
        final order = state.order;
        return Scaffold(
          appBar: AppBar(
            title: Text(order?.orderCode ?? 'Order Detail'),
            actions: [
              IconButton(
                onPressed:
                    state.isBusy
                        ? null
                        : () =>
                            context.read<OrderDetailCubit>().refreshTracking(),
                icon: const Icon(Icons.refresh),
              ),
            ],
          ),
          body:
              state.status == OrderDetailStatus.loading && order == null
                  ? const Center(child: CircularProgressIndicator())
                  : order == null
                  ? Center(
                    child: Padding(
                      padding: const EdgeInsets.all(24),
                      child: Text(
                        state.errorMessage ?? 'Unable to load order detail.',
                        textAlign: TextAlign.center,
                      ),
                    ),
                  )
                  : RefreshIndicator(
                    onRefresh:
                        () =>
                            context.read<OrderDetailCubit>().refreshTracking(),
                    child: ListView(
                      padding: const EdgeInsets.all(16),
                      children: [
                        Card(
                          child: Padding(
                            padding: const EdgeInsets.all(16),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  order.restaurantName,
                                  style:
                                      Theme.of(context).textTheme.titleMedium,
                                ),
                                const SizedBox(height: 8),
                                Wrap(
                                  spacing: 8,
                                  runSpacing: 8,
                                  children: [
                                    FoodyaStatusChip(
                                      value: order.status,
                                      icon: Icons.delivery_dining_outlined,
                                    ),
                                    Chip(
                                      label: Text(
                                        _paymentStatusLabel(
                                          order.paymentStatus,
                                        ),
                                      ),
                                    ),
                                    Chip(
                                      label: Text(
                                        _paymentMethodLabel(
                                          order.paymentMethod,
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                                const SizedBox(height: 12),
                                Text(order.deliveryAddress),
                                const SizedBox(height: 8),
                                Text(
                                  'Subtotal ${order.subtotalAmount.toStringAsFixed(0)} VND',
                                ),
                                Text(
                                  'Delivery fee ${order.deliveryFee.toStringAsFixed(0)} VND',
                                ),
                                Text(
                                  'Total ${order.totalAmount.toStringAsFixed(0)} VND',
                                  style: Theme.of(context).textTheme.titleSmall,
                                ),
                              ],
                            ),
                          ),
                        ),
                        const SizedBox(height: 16),
                        _DeliveryTrackingSection(
                          order: order,
                          state: state,
                          onRefresh:
                              () =>
                                  context
                                      .read<OrderDetailCubit>()
                                      .refreshTracking(),
                          onLiveChanged:
                              (enabled) => context
                                  .read<OrderDetailCubit>()
                                  .toggleLiveTracking(enabled),
                        ),
                        if (order.status.toUpperCase() == 'SUCCESS') ...[
                          const SizedBox(height: 16),
                          _ReviewForm(
                            isSubmitting:
                                state.status == OrderDetailStatus.reviewing,
                          ),
                        ],
                        const SizedBox(height: 16),
                        if (order.status.toUpperCase() != 'CANCELLED')
                          FilledButton.tonal(
                            onPressed:
                                state.isBusy
                                    ? null
                                    : () => _cancelOrder(context),
                            child: const Text('Cancel order'),
                          ),
                      ],
                    ),
                  ),
        );
      },
    );
  }

  void _cancelOrder(BuildContext context) {
    showDialog<void>(
      context: context,
      builder:
          (dialogContext) => AlertDialog(
            title: const Text('Cancel order?'),
            content: const Text('You can optionally add a cancel reason.'),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(dialogContext).pop(),
                child: const Text('Keep order'),
              ),
              FilledButton(
                onPressed: () {
                  Navigator.of(dialogContext).pop();
                  context.read<OrderDetailCubit>().cancelOrder();
                },
                child: const Text('Cancel'),
              ),
            ],
          ),
    );
  }
}

class _DeliveryTrackingSection extends StatelessWidget {
  const _DeliveryTrackingSection({
    required this.order,
    required this.state,
    required this.onRefresh,
    required this.onLiveChanged,
  });

  final OrderDetail order;
  final OrderDetailState state;
  final Future<void> Function() onRefresh;
  final ValueChanged<bool> onLiveChanged;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final points = state.trackingPoints;
    final latest = points.isEmpty ? null : points.last;
    final canLiveTrack = _canLiveTrack(order.status);

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    'Live delivery tracking',
                    style: theme.textTheme.titleSmall,
                  ),
                ),
                IconButton(
                  tooltip: 'Refresh tracking',
                  onPressed: state.isTrackingLoading ? null : onRefresh,
                  icon:
                      state.isTrackingLoading
                          ? const SizedBox(
                            width: 20,
                            height: 20,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                          : const Icon(Icons.refresh),
                ),
              ],
            ),
            const SizedBox(height: 8),
            _TrackingStatusRow(
              orderStatus: order.status,
              latest: latest,
              pointCount: points.length,
              lastTrackingRefreshAt: state.lastTrackingRefreshAt,
            ),
            const SizedBox(height: 12),
            SwitchListTile.adaptive(
              contentPadding: EdgeInsets.zero,
              title: const Text('Live updates'),
              subtitle: Text(
                canLiveTrack
                    ? 'Realtime when connected, fallback refresh if offline.'
                    : 'Available after the order is assigned to delivery.',
              ),
              value: state.isLiveTrackingEnabled,
              onChanged: canLiveTrack ? onLiveChanged : null,
            ),
            const SizedBox(height: 12),
            SizedBox(
              height: 220,
              width: double.infinity,
              child: DecoratedBox(
                decoration: BoxDecoration(
                  color: theme.colorScheme.surfaceContainerHighest,
                  borderRadius: BorderRadius.circular(8),
                ),
                child:
                    points.isEmpty
                        ? Center(
                          child: Padding(
                            padding: const EdgeInsets.all(16),
                            child: Text(
                              state.isTrackingLoading
                                  ? 'Loading tracking updates...'
                                  : 'No tracking updates yet.',
                              textAlign: TextAlign.center,
                              style: theme.textTheme.bodyMedium,
                            ),
                          ),
                        )
                        : CustomPaint(
                          painter: _TrackingRoutePainter(
                            points: points,
                            colorScheme: theme.colorScheme,
                          ),
                          child: const SizedBox.expand(),
                        ),
              ),
            ),
            if (points.isNotEmpty) ...[
              const SizedBox(height: 16),
              Text('Route timeline', style: theme.textTheme.titleSmall),
              const SizedBox(height: 8),
              ...points.reversed
                  .take(5)
                  .map((point) => _TrackingTimelineTile(point: point)),
            ],
          ],
        ),
      ),
    );
  }

  bool _canLiveTrack(String status) {
    return switch (status.toUpperCase()) {
      'ASSIGNED' || 'PREPARING' || 'DELIVERING' => true,
      _ => false,
    };
  }
}

class _TrackingStatusRow extends StatelessWidget {
  const _TrackingStatusRow({
    required this.orderStatus,
    required this.latest,
    required this.pointCount,
    required this.lastTrackingRefreshAt,
  });

  final String orderStatus;
  final OrderTrackingPoint? latest;
  final int pointCount;
  final DateTime? lastTrackingRefreshAt;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: [
        Chip(
          avatar: const Icon(Icons.local_shipping_outlined, size: 18),
          label: Text(friendlyStatusLabel(orderStatus)),
        ),
        Chip(
          avatar: const Icon(Icons.route_outlined, size: 18),
          label: Text('$pointCount update${pointCount == 1 ? '' : 's'}'),
        ),
        if (latest != null)
          Chip(
            avatar: const Icon(Icons.my_location_outlined, size: 18),
            label: Text(
              '${latest!.lat.toStringAsFixed(5)}, '
              '${latest!.lng.toStringAsFixed(5)}',
            ),
          ),
        if (lastTrackingRefreshAt != null)
          Chip(
            avatar: const Icon(Icons.schedule_outlined, size: 18),
            label: Text(_formatClock(lastTrackingRefreshAt!.toLocal())),
          ),
        if (latest == null)
          Text(
            'Waiting for the first delivery location.',
            style: theme.textTheme.bodyMedium,
          ),
      ],
    );
  }
}

class _TrackingTimelineTile extends StatelessWidget {
  const _TrackingTimelineTile({required this.point});

  final OrderTrackingPoint point;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: EdgeInsets.zero,
      dense: true,
      leading: const Icon(Icons.location_on_outlined),
      title: Text(
        '${point.lat.toStringAsFixed(5)}, ${point.lng.toStringAsFixed(5)}',
      ),
      subtitle: Text(_formatDateTime(point.recordedAt.toLocal())),
    );
  }
}

class _TrackingRoutePainter extends CustomPainter {
  const _TrackingRoutePainter({
    required this.points,
    required this.colorScheme,
  });

  final List<OrderTrackingPoint> points;
  final ColorScheme colorScheme;

  @override
  void paint(Canvas canvas, Size size) {
    final route = _project(points, size);
    final gridPaint =
        Paint()
          ..color = colorScheme.outlineVariant.withValues(alpha: 0.55)
          ..strokeWidth = 1;

    for (var i = 1; i < 4; i++) {
      final dx = size.width * i / 4;
      final dy = size.height * i / 4;
      canvas.drawLine(Offset(dx, 0), Offset(dx, size.height), gridPaint);
      canvas.drawLine(Offset(0, dy), Offset(size.width, dy), gridPaint);
    }

    if (route.isEmpty) {
      return;
    }

    final routePaint =
        Paint()
          ..color = colorScheme.primary
          ..strokeWidth = 4
          ..strokeCap = StrokeCap.round
          ..strokeJoin = StrokeJoin.round
          ..style = PaintingStyle.stroke;

    if (route.length > 1) {
      final path = Path()..moveTo(route.first.dx, route.first.dy);
      for (final point in route.skip(1)) {
        path.lineTo(point.dx, point.dy);
      }
      canvas.drawPath(path, routePaint);
    }

    final startPaint = Paint()..color = colorScheme.secondary;
    final latestPaint = Paint()..color = colorScheme.error;
    final pointPaint = Paint()..color = colorScheme.primary;

    for (var i = 0; i < route.length; i++) {
      final paint =
          i == 0
              ? startPaint
              : i == route.length - 1
              ? latestPaint
              : pointPaint;
      canvas.drawCircle(route[i], i == route.length - 1 ? 8 : 5, paint);
    }
  }

  List<Offset> _project(List<OrderTrackingPoint> points, Size size) {
    const padding = 24.0;
    final minLat = points.map((point) => point.lat).reduce(math.min);
    final maxLat = points.map((point) => point.lat).reduce(math.max);
    final minLng = points.map((point) => point.lng).reduce(math.min);
    final maxLng = points.map((point) => point.lng).reduce(math.max);
    final latRange = math.max(maxLat - minLat, 0.00001);
    final lngRange = math.max(maxLng - minLng, 0.00001);
    final width = math.max(size.width - padding * 2, 1);
    final height = math.max(size.height - padding * 2, 1);

    return points
        .map(
          (point) => Offset(
            padding + ((point.lng - minLng) / lngRange) * width,
            padding + (1 - ((point.lat - minLat) / latRange)) * height,
          ),
        )
        .toList(growable: false);
  }

  @override
  bool shouldRepaint(covariant _TrackingRoutePainter oldDelegate) {
    return oldDelegate.points != points ||
        oldDelegate.colorScheme != colorScheme;
  }
}

String _formatClock(DateTime value) {
  return '${value.hour.toString().padLeft(2, '0')}:'
      '${value.minute.toString().padLeft(2, '0')}';
}

String _formatDateTime(DateTime value) {
  return '${value.year.toString().padLeft(4, '0')}-'
      '${value.month.toString().padLeft(2, '0')}-'
      '${value.day.toString().padLeft(2, '0')} '
      '${_formatClock(value)}';
}

String _paymentMethodLabel(String value) {
  return switch (value.toUpperCase()) {
    'COD' => 'Cash on delivery',
    _ => value,
  };
}

String _paymentStatusLabel(String value) {
  return switch (value.toUpperCase()) {
    'UNPAID' => 'Payment pending',
    'PAID' => 'Payment collected',
    'FAILED' => 'Payment failed',
    'REFUNDED' => 'Payment refunded',
    _ => value,
  };
}

class _ReviewForm extends StatefulWidget {
  const _ReviewForm({required this.isSubmitting});

  final bool isSubmitting;

  @override
  State<_ReviewForm> createState() => _ReviewFormState();
}

class _ReviewFormState extends State<_ReviewForm> {
  int _stars = 5;
  final _commentController = TextEditingController();

  @override
  void dispose() {
    _commentController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Review order', style: Theme.of(context).textTheme.titleSmall),
            const SizedBox(height: 8),
            DropdownButtonFormField<int>(
              value: _stars,
              decoration: const InputDecoration(labelText: 'Stars'),
              items: [1, 2, 3, 4, 5]
                  .map(
                    (value) => DropdownMenuItem<int>(
                      value: value,
                      child: Text('$value'),
                    ),
                  )
                  .toList(growable: false),
              onChanged:
                  widget.isSubmitting
                      ? null
                      : (value) => setState(() => _stars = value ?? 5),
            ),
            const SizedBox(height: 8),
            TextField(
              controller: _commentController,
              decoration: const InputDecoration(labelText: 'Comment'),
              maxLines: 3,
              maxLength: 1000,
            ),
            const SizedBox(height: 8),
            FilledButton.icon(
              onPressed:
                  widget.isSubmitting
                      ? null
                      : () => context.read<OrderDetailCubit>().submitReview(
                        stars: _stars,
                        comment: _commentController.text,
                      ),
              icon:
                  widget.isSubmitting
                      ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                      : const Icon(Icons.rate_review_outlined),
              label: const Text('Submit review'),
            ),
          ],
        ),
      ),
    );
  }
}
