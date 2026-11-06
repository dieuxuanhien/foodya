import 'package:geolocator/geolocator.dart';

import '../../domain/services/delivery_position_provider.dart';

class GeolocatorDeliveryPositionProvider implements DeliveryPositionProvider {
  const GeolocatorDeliveryPositionProvider();

  @override
  Future<DeliveryPositionResult> getCurrentPosition() async {
    try {
      if (!await Geolocator.isLocationServiceEnabled()) {
        return const DeliveryPositionResult.failure(
          DeliveryPositionFailure.serviceDisabled,
        );
      }
      var permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
      }
      if (permission == LocationPermission.deniedForever) {
        return const DeliveryPositionResult.failure(
          DeliveryPositionFailure.permissionPermanentlyDenied,
        );
      }
      if (permission == LocationPermission.denied) {
        return const DeliveryPositionResult.failure(
          DeliveryPositionFailure.permissionDenied,
        );
      }
      final position = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
        timeLimit: const Duration(seconds: 10),
      );
      return DeliveryPositionResult.position(
        position.latitude,
        position.longitude,
      );
    } catch (_) {
      return const DeliveryPositionResult.failure(
        DeliveryPositionFailure.unavailable,
      );
    }
  }
}

class DemoDeliveryPositionProvider implements DeliveryPositionProvider {
  const DemoDeliveryPositionProvider({this.lat = 10.7769, this.lng = 106.7009});
  final double lat;
  final double lng;

  @override
  Future<DeliveryPositionResult> getCurrentPosition() async =>
      DeliveryPositionResult.position(lat, lng);
}
