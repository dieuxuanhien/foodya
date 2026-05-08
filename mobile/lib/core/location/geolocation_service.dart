import 'package:geolocator/geolocator.dart';
import 'package:geocoding/geocoding.dart';

class GeolocationService {
  Future<bool> isLocationServiceEnabled() async {
    return Geolocator.isLocationServiceEnabled();
  }

  Future<bool> isLocationPermissionGranted() async {
    final permission = await Geolocator.checkPermission();
    return permission == LocationPermission.always ||
        permission == LocationPermission.whileInUse;
  }

  Future<LocationPermission> requestPermission() async {
    return Geolocator.requestPermission();
  }

  Future<Position> getCurrentPosition({
    LocationAccuracy accuracy = LocationAccuracy.high,
    Duration? timeLimit,
  }) async {
    return Geolocator.getCurrentPosition(
      desiredAccuracy: accuracy,
      timeLimit: timeLimit,
    );
  }

  Future<String?> getFriendlyAddress(Position position) async {
    try {
      final placemarks = await placemarkFromCoordinates(
        position.latitude,
        position.longitude,
      );

      if (placemarks.isEmpty) {
        return null;
      }

      final placemark = placemarks.first;
      final parts = _collectAddressParts([
        placemark.subAdministrativeArea,
        placemark.administrativeArea,
      ]);

      if (parts.isNotEmpty) {
        return parts.join(', ');
      }

      final fallbackParts = _collectAddressParts([
        placemark.locality,
        placemark.country,
      ]);

      if (fallbackParts.isNotEmpty) {
        return fallbackParts.join(', ');
      }
    } catch (_) {
      return null;
    }

    return null;
  }

  List<String> _collectAddressParts(List<String?> parts) {
    return parts
        .where((part) => part != null && part.trim().isNotEmpty)
        .map((part) => part!.trim())
        .toList(growable: false);
  }
}
