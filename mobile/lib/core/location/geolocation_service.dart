import 'package:geolocator/geolocator.dart';

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
}
