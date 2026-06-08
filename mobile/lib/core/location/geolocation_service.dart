import 'package:geolocator/geolocator.dart';

import '../auth/auth_session_recovery.dart';
import '../network/api_exception.dart';
import 'location_address_remote_data_source.dart';

class GeolocationService {
  GeolocationService({
    required LocationAddressRemoteDataSource remoteDataSource,
    required AuthSessionRecovery sessionRecovery,
  }) : _remoteDataSource = remoteDataSource,
       _sessionRecovery = sessionRecovery;

  final LocationAddressRemoteDataSource _remoteDataSource;
  final AuthSessionRecovery _sessionRecovery;

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
    return getFriendlyAddressForCoordinates(
      lat: position.latitude,
      lng: position.longitude,
    );
  }

  Future<String?> getFriendlyAddressForCoordinates({
    required double lat,
    required double lng,
  }) async {
    try {
      final resolved = await _sessionRecovery.runAuthorized((accessToken) {
        return _remoteDataSource.resolveAddress(
          lat: lat,
          lng: lng,
          accessToken: accessToken,
        );
      });

      final address = resolved.address.trim();
      return address.isEmpty ? null : address;
    } on ApiException {
      return null;
    } catch (_) {
      return null;
    }
  }
}
