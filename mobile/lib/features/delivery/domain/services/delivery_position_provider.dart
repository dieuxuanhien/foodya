import 'package:equatable/equatable.dart';

enum DeliveryPositionFailure {
  serviceDisabled,
  permissionDenied,
  permissionPermanentlyDenied,
  unavailable,
}

class DeliveryPositionResult extends Equatable {
  const DeliveryPositionResult.position(this.lat, this.lng) : failure = null;
  const DeliveryPositionResult.failure(this.failure) : lat = null, lng = null;

  final double? lat;
  final double? lng;
  final DeliveryPositionFailure? failure;

  bool get hasPosition => lat != null && lng != null;

  @override
  List<Object?> get props => [lat, lng, failure];
}

abstract class DeliveryPositionProvider {
  Future<DeliveryPositionResult> getCurrentPosition();
}
