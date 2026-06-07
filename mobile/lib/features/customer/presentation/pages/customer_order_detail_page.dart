import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter/semantics.dart';

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
                        if (_isDeliveringOrder(order.status)) ...[
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
                        ],
                        if (order.status.toUpperCase() == 'SUCCESS') ...[
                          const SizedBox(height: 16),
                          _ReviewForm(
                            isSubmitting:
                                state.status == OrderDetailStatus.reviewing,
                          ),
                        ],
                        if (_canCancelOrder(order.status)) ...[
                          const SizedBox(height: 16),
                          FilledButton.tonal(
                            onPressed:
                                state.isBusy
                                    ? null
                                    : () => _cancelOrder(context),
                            child: const Text('Cancel order'),
                          ),
                        ],
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
    final destination = _TrackingMapCoordinate.fromOrder(order);
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
                    : 'Available while the courier is delivering.',
              ),
              value: state.isLiveTrackingEnabled,
              onChanged: canLiveTrack ? onLiveChanged : null,
            ),
            const SizedBox(height: 12),
            SizedBox(
              height: 220,
              width: double.infinity,
              child: ClipRRect(
                borderRadius: BorderRadius.circular(8),
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    color: theme.colorScheme.surfaceContainerHighest,
                  ),
                  child: Stack(
                    children: [
                      CustomPaint(
                        painter: _TrackingRoutePainter(
                          points: points,
                          destination: destination,
                          colorScheme: theme.colorScheme,
                        ),
                        child: const SizedBox.expand(),
                      ),
                      if (points.isEmpty)
                        Center(
                          child: Padding(
                            padding: const EdgeInsets.all(16),
                            child: DecoratedBox(
                              decoration: BoxDecoration(
                                color: theme.colorScheme.surface.withValues(
                                  alpha: 0.88,
                                ),
                                borderRadius: BorderRadius.circular(8),
                              ),
                              child: Padding(
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 12,
                                  vertical: 8,
                                ),
                                child: Text(
                                  state.isTrackingLoading
                                      ? 'Loading tracking updates...'
                                      : 'No tracking updates yet.',
                                  textAlign: TextAlign.center,
                                  style: theme.textTheme.bodyMedium,
                                ),
                              ),
                            ),
                          ),
                        ),
                      if (latest != null)
                        const Positioned(
                          left: 10,
                          top: 10,
                          child: _TrackingMapPill(
                            icon: Icons.delivery_dining,
                            label: 'Courier',
                          ),
                        ),
                      if (destination != null)
                        const Positioned(
                          right: 10,
                          bottom: 10,
                          child: _TrackingMapPill(
                            icon: Icons.location_pin,
                            label: 'Destination',
                          ),
                        ),
                    ],
                  ),
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
      'DELIVERING' => true,
      _ => false,
    };
  }
}

class _TrackingMapPill extends StatelessWidget {
  const _TrackingMapPill({required this.icon, required this.label});

  final IconData icon;
  final String label;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return DecoratedBox(
      decoration: BoxDecoration(
        color: theme.colorScheme.surface.withValues(alpha: 0.9),
        borderRadius: BorderRadius.circular(999),
        border: Border.all(
          color: theme.colorScheme.outlineVariant.withValues(alpha: 0.7),
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 5),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 16),
            const SizedBox(width: 4),
            Text(label, style: theme.textTheme.labelSmall),
          ],
        ),
      ),
    );
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
    required this.destination,
    required this.colorScheme,
  });

  final List<OrderTrackingPoint> points;
  final _TrackingMapCoordinate? destination;
  final ColorScheme colorScheme;

  @override
  void paint(Canvas canvas, Size size) {
    _paintSchematicMap(canvas, size);

    final route = _projectRoute(points, size);
    final destinationOffset =
        destination == null ? null : _project(destination!, size);

    if (route.isEmpty && destinationOffset == null) {
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

    if (route.isNotEmpty && destinationOffset != null) {
      final remainingPaint =
          Paint()
            ..color = colorScheme.primary.withValues(alpha: 0.35)
            ..strokeWidth = 2
            ..strokeCap = StrokeCap.round
            ..style = PaintingStyle.stroke;
      canvas.drawLine(route.last, destinationOffset, remainingPaint);
    }

    final originPaint = Paint()..color = colorScheme.secondary;
    final pointPaint =
        Paint()..color = colorScheme.primary.withValues(alpha: 0.7);

    for (var i = 0; i < route.length; i++) {
      final isLatest = i == route.length - 1;
      if (isLatest) {
        continue;
      }
      canvas.drawCircle(
        route[i],
        i == 0 ? 6 : 4,
        i == 0 ? originPaint : pointPaint,
      );
    }

    if (destinationOffset != null) {
      _drawIconMarker(
        canvas,
        destinationOffset,
        Icons.location_pin,
        background: colorScheme.tertiaryContainer,
        foreground: colorScheme.onTertiaryContainer,
      );
    }

    if (route.isNotEmpty) {
      _drawIconMarker(
        canvas,
        route.last,
        Icons.delivery_dining,
        background: colorScheme.primary,
        foreground: colorScheme.onPrimary,
      );
    }
  }

  void _paintSchematicMap(Canvas canvas, Size size) {
    final basePaint = Paint()..color = colorScheme.surfaceContainerHighest;
    canvas.drawRect(Offset.zero & size, basePaint);

    final parkPaint =
        Paint()..color = colorScheme.secondaryContainer.withValues(alpha: 0.28);
    canvas.drawRRect(
      RRect.fromRectAndRadius(
        Rect.fromLTWH(
          size.width * 0.05,
          size.height * 0.08,
          size.width * 0.28,
          size.height * 0.28,
        ),
        const Radius.circular(18),
      ),
      parkPaint,
    );
    canvas.drawRRect(
      RRect.fromRectAndRadius(
        Rect.fromLTWH(
          size.width * 0.68,
          size.height * 0.58,
          size.width * 0.24,
          size.height * 0.24,
        ),
        const Radius.circular(16),
      ),
      parkPaint,
    );

    final blockPaint =
        Paint()..color = colorScheme.outlineVariant.withValues(alpha: 0.42);
    for (final block in <Rect>[
      Rect.fromLTWH(size.width * 0.43, size.height * 0.10, 26, 42),
      Rect.fromLTWH(size.width * 0.54, size.height * 0.18, 46, 24),
      Rect.fromLTWH(size.width * 0.15, size.height * 0.58, 36, 52),
      Rect.fromLTWH(size.width * 0.77, size.height * 0.20, 32, 48),
      Rect.fromLTWH(size.width * 0.47, size.height * 0.70, 54, 28),
    ]) {
      canvas.drawRRect(
        RRect.fromRectAndRadius(block, const Radius.circular(4)),
        blockPaint,
      );
    }

    final roadPaint =
        Paint()
          ..color = colorScheme.surface.withValues(alpha: 0.95)
          ..strokeWidth = 18
          ..strokeCap = StrokeCap.round
          ..strokeJoin = StrokeJoin.round
          ..style = PaintingStyle.stroke;
    final roadOutlinePaint =
        Paint()
          ..color = colorScheme.outlineVariant.withValues(alpha: 0.45)
          ..strokeWidth = 20
          ..strokeCap = StrokeCap.round
          ..strokeJoin = StrokeJoin.round
          ..style = PaintingStyle.stroke;
    final mainRoad =
        Path()
          ..moveTo(-10, size.height * 0.70)
          ..cubicTo(
            size.width * 0.25,
            size.height * 0.56,
            size.width * 0.45,
            size.height * 0.48,
            size.width + 10,
            size.height * 0.34,
          );
    canvas.drawPath(mainRoad, roadOutlinePaint);
    canvas.drawPath(mainRoad, roadPaint);

    final sideRoadPaint =
        Paint()
          ..color = colorScheme.surface.withValues(alpha: 0.76)
          ..strokeWidth = 10
          ..strokeCap = StrokeCap.round
          ..style = PaintingStyle.stroke;
    canvas.drawLine(
      Offset(size.width * 0.30, -10),
      Offset(size.width * 0.62, size.height + 10),
      sideRoadPaint,
    );
    canvas.drawLine(
      Offset(-10, size.height * 0.25),
      Offset(size.width * 0.82, size.height * 0.88),
      sideRoadPaint,
    );
  }

  List<Offset> _projectRoute(List<OrderTrackingPoint> points, Size size) {
    return points
        .map(
          (point) =>
              _project(_TrackingMapCoordinate(point.lat, point.lng), size),
        )
        .toList(growable: false);
  }

  Offset _project(_TrackingMapCoordinate point, Size size) {
    final bounds = _bounds();
    const padding = 24.0;
    final width = math.max(size.width - padding * 2, 1);
    final height = math.max(size.height - padding * 2, 1);

    return Offset(
      padding + ((point.lng - bounds.minLng) / bounds.lngRange) * width,
      padding + (1 - ((point.lat - bounds.minLat) / bounds.latRange)) * height,
    );
  }

  _TrackingBounds _bounds() {
    final coordinates = <_TrackingMapCoordinate>[
      ...points.map((point) => _TrackingMapCoordinate(point.lat, point.lng)),
      if (destination != null) destination!,
    ];

    if (coordinates.isEmpty) {
      return const _TrackingBounds(0, 0.001, 0, 0.001);
    }

    var minLat = coordinates.first.lat;
    var maxLat = coordinates.first.lat;
    var minLng = coordinates.first.lng;
    var maxLng = coordinates.first.lng;
    for (final point in coordinates.skip(1)) {
      minLat = math.min(minLat, point.lat);
      maxLat = math.max(maxLat, point.lat);
      minLng = math.min(minLng, point.lng);
      maxLng = math.max(maxLng, point.lng);
    }

    if ((maxLat - minLat).abs() < 0.00001) {
      minLat -= 0.0005;
      maxLat += 0.0005;
    }
    if ((maxLng - minLng).abs() < 0.00001) {
      minLng -= 0.0005;
      maxLng += 0.0005;
    }

    return _TrackingBounds(minLat, maxLat, minLng, maxLng);
  }

  void _drawIconMarker(
    Canvas canvas,
    Offset center,
    IconData icon, {
    required Color background,
    required Color foreground,
  }) {
    final shadowPaint = Paint()..color = Colors.black.withValues(alpha: 0.12);
    canvas.drawCircle(center.translate(0, 2), 17, shadowPaint);
    canvas.drawCircle(center, 16, Paint()..color = background);

    final iconPainter = TextPainter(
      text: TextSpan(
        text: String.fromCharCode(icon.codePoint),
        style: TextStyle(
          color: foreground,
          fontFamily: icon.fontFamily,
          fontSize: 20,
          package: icon.fontPackage,
        ),
      ),
      textDirection: TextDirection.ltr,
    )..layout();
    iconPainter.paint(
      canvas,
      center - Offset(iconPainter.width / 2, iconPainter.height / 2),
    );
  }

  @override
  bool shouldRepaint(covariant _TrackingRoutePainter oldDelegate) {
    return oldDelegate.points != points ||
        oldDelegate.destination != destination ||
        oldDelegate.colorScheme != colorScheme;
  }

  @override
  SemanticsBuilderCallback get semanticsBuilder {
    return (Size size) {
      final semantics = <CustomPainterSemantics>[];
      if (destination != null) {
        final center = _project(destination!, size);
        semantics.add(
          CustomPainterSemantics(
            rect: Rect.fromCircle(center: center, radius: 18),
            properties: const SemanticsProperties(
              label: 'Destination marker',
              textDirection: TextDirection.ltr,
            ),
          ),
        );
      }
      if (points.isNotEmpty) {
        final center = _projectRoute(points, size).last;
        semantics.add(
          CustomPainterSemantics(
            rect: Rect.fromCircle(center: center, radius: 18),
            properties: const SemanticsProperties(
              label: 'Courier marker',
              textDirection: TextDirection.ltr,
            ),
          ),
        );
      }
      return semantics;
    };
  }

  @override
  bool shouldRebuildSemantics(covariant _TrackingRoutePainter oldDelegate) {
    return oldDelegate.points != points ||
        oldDelegate.destination != destination ||
        oldDelegate.colorScheme != colorScheme;
  }
}

class _TrackingMapCoordinate {
  const _TrackingMapCoordinate(this.lat, this.lng);

  final double lat;
  final double lng;

  static _TrackingMapCoordinate? fromOrder(OrderDetail order) {
    final lat = order.deliveryLatitude;
    final lng = order.deliveryLongitude;
    if (lat == null || lng == null) {
      return null;
    }
    return _TrackingMapCoordinate(lat, lng);
  }
}

class _TrackingBounds {
  const _TrackingBounds(this.minLat, this.maxLat, this.minLng, this.maxLng);

  final double minLat;
  final double maxLat;
  final double minLng;
  final double maxLng;

  double get latRange => maxLat - minLat;

  double get lngRange => maxLng - minLng;
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

bool _isDeliveringOrder(String status) => status.toUpperCase() == 'DELIVERING';

bool _canCancelOrder(String status) {
  return switch (status.toUpperCase()) {
    'PENDING' || 'ACCEPTED' || 'ASSIGNED' => true,
    _ => false,
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
