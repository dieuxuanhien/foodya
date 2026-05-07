import 'package:flutter_test/flutter_test.dart';
import 'package:foodya_mobile/core/network/api_error_ui_message.dart';
import 'package:foodya_mobile/core/network/api_exception.dart';

void main() {
  group('ApiErrorUiMessageMapper', () {
    test('maps validation details to field errors', () {
      final error = ApiException(
        statusCode: 422,
        code: 'VALIDATION_FAILED',
        message: 'Request validation failed',
        details: {
          'email': 'already exists',
          'phoneNumber': 'must be a valid phone number',
        },
      );

      final mapped = ApiErrorUiMessageMapper.fromException(
        error,
        fallbackMessage: 'Unable to register account.',
      );

      expect(
        mapped.message,
        'Please check the highlighted fields and try again.',
      );
      expect(mapped.fieldErrors['email'], 'Email already exists');
      expect(
        mapped.fieldErrors['phoneNumber'],
        'Phone number must be a valid phone number',
      );
    });

    test('maps unauthorized auth errors to session expiry copy', () {
      final error = ApiException(
        statusCode: 401,
        code: 'UNAUTHORIZED',
        message: 'invalid credentials',
      );

      final mapped = ApiErrorUiMessageMapper.fromException(
        error,
        fallbackMessage: 'Unable to login.',
      );

      expect(mapped.message, 'Your session has expired. Please log in again.');
    });

    test('maps forbidden account state to approval copy', () {
      final error = ApiException(
        statusCode: 403,
        code: 'FORBIDDEN',
        message: 'account is not active',
      );

      final mapped = ApiErrorUiMessageMapper.fromException(
        error,
        fallbackMessage: 'Unable to login.',
      );

      expect(
        mapped.message,
        'Your account is not active yet. Please wait for approval.',
      );
    });
  });
}
