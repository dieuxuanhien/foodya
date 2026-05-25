import 'package:flutter/material.dart';

class ManualLocationInput {
  const ManualLocationInput({required this.latitude, required this.longitude});

  final double latitude;
  final double longitude;
}

Future<ManualLocationInput?> showManualLocationSheet({
  required BuildContext context,
  double? initialLatitude,
  double? initialLongitude,
}) {
  return showModalBottomSheet<ManualLocationInput>(
    context: context,
    isScrollControlled: true,
    builder:
        (context) => _ManualLocationSheet(
          initialLatitude: initialLatitude,
          initialLongitude: initialLongitude,
        ),
  );
}

class _ManualLocationSheet extends StatefulWidget {
  const _ManualLocationSheet({
    required this.initialLatitude,
    required this.initialLongitude,
  });

  final double? initialLatitude;
  final double? initialLongitude;

  @override
  State<_ManualLocationSheet> createState() => _ManualLocationSheetState();
}

class _ManualLocationSheetState extends State<_ManualLocationSheet> {
  late final TextEditingController _latController;
  late final TextEditingController _lngController;
  String? _error;

  @override
  void initState() {
    super.initState();
    _latController = TextEditingController(
      text: widget.initialLatitude?.toStringAsFixed(7) ?? '',
    );
    _lngController = TextEditingController(
      text: widget.initialLongitude?.toStringAsFixed(7) ?? '',
    );
  }

  @override
  void dispose() {
    _latController.dispose();
    _lngController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final bottomInset = MediaQuery.viewInsetsOf(context).bottom;
    return SafeArea(
      child: Padding(
        padding: EdgeInsets.fromLTRB(16, 16, 16, bottomInset + 16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              'Enter coordinates',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _latController,
              decoration: const InputDecoration(
                labelText: 'Latitude',
                hintText: '10.762622',
              ),
              keyboardType: const TextInputType.numberWithOptions(
                signed: true,
                decimal: true,
              ),
              textInputAction: TextInputAction.next,
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _lngController,
              decoration: const InputDecoration(
                labelText: 'Longitude',
                hintText: '106.660172',
              ),
              keyboardType: const TextInputType.numberWithOptions(
                signed: true,
                decimal: true,
              ),
              textInputAction: TextInputAction.done,
              onSubmitted: (_) => _submit(),
            ),
            if (_error != null) ...[
              const SizedBox(height: 8),
              Text(
                _error!,
                style: TextStyle(color: Theme.of(context).colorScheme.error),
              ),
            ],
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: TextButton(
                    onPressed: () => Navigator.of(context).pop(),
                    child: const Text('Cancel'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: FilledButton(
                    onPressed: _submit,
                    child: const Text('Apply'),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  void _submit() {
    final lat = double.tryParse(_latController.text.trim());
    final lng = double.tryParse(_lngController.text.trim());
    if (lat == null || lat < -90 || lat > 90) {
      setState(() => _error = 'Latitude must be between -90 and 90.');
      return;
    }
    if (lng == null || lng < -180 || lng > 180) {
      setState(() => _error = 'Longitude must be between -180 and 180.');
      return;
    }
    Navigator.of(
      context,
    ).pop(ManualLocationInput(latitude: lat, longitude: lng));
  }
}
