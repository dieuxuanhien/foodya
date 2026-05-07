import 'api_exception.dart';

class ApiErrorUiMessage {
  const ApiErrorUiMessage({required this.message, this.fieldErrors = const {}});

  final String message;
  final Map<String, String> fieldErrors;
}

class ApiErrorUiMessageMapper {
  const ApiErrorUiMessageMapper._();

  static ApiErrorUiMessage fromException(
    ApiException error, {
    required String fallbackMessage,
  }) {
    final fieldErrors = _extractFieldErrors(error.details);

    if (fieldErrors.isNotEmpty) {
      return ApiErrorUiMessage(
        message: _validationMessage(fieldErrors, error.message),
        fieldErrors: fieldErrors,
      );
    }

    final code = error.code?.toUpperCase();
    final message = error.message.toLowerCase();

    switch (code) {
      case 'UNAUTHORIZED':
      case 'NO_REFRESH_TOKEN':
      case 'SESSION_EXPIRED':
        return const ApiErrorUiMessage(
          message: 'Your session has expired. Please log in again.',
        );
      case 'FORBIDDEN':
        if (message.contains('account is not active')) {
          return const ApiErrorUiMessage(
            message:
                'Your account is not active yet. Please wait for approval.',
          );
        }
        if (message.contains('another user') ||
            message.contains('cannot revoke')) {
          return const ApiErrorUiMessage(
            message: 'You can only manage your own sessions.',
          );
        }
        return const ApiErrorUiMessage(
          message: 'You do not have permission to perform this action.',
        );
      case 'CONFLICT':
        return const ApiErrorUiMessage(
          message:
              'This information is already in use. Please check your input and try again.',
        );
      case 'RATE_LIMITED':
        return const ApiErrorUiMessage(
          message: 'Too many requests. Please wait a moment and try again.',
        );
      case 'NETWORK_TIMEOUT':
        return const ApiErrorUiMessage(
          message:
              'The request timed out. Check your connection and try again.',
        );
      case 'NETWORK_ERROR':
        return const ApiErrorUiMessage(
          message: 'Cannot reach the server. Check your network and try again.',
        );
      case 'MALFORMED_RESPONSE':
        return const ApiErrorUiMessage(
          message:
              'The server returned an unexpected response. Please try again.',
        );
      case 'INTERNAL_ERROR':
        return const ApiErrorUiMessage(
          message: 'Something went wrong on our side. Please try again later.',
        );
      default:
        break;
    }

    if (error.statusCode >= 500) {
      return const ApiErrorUiMessage(
        message: 'Something went wrong on our side. Please try again later.',
      );
    }

    if (error.statusCode == 401) {
      return const ApiErrorUiMessage(
        message: 'Your session has expired. Please log in again.',
      );
    }

    if (error.statusCode == 403) {
      return const ApiErrorUiMessage(
        message: 'You do not have permission to perform this action.',
      );
    }

    if (error.statusCode == 404) {
      return const ApiErrorUiMessage(
        message: 'The requested item was not found.',
      );
    }

    if (error.statusCode == 409) {
      return const ApiErrorUiMessage(
        message:
            'This information is already in use. Please check your input and try again.',
      );
    }

    if (error.statusCode == 422) {
      return ApiErrorUiMessage(
        message: _validationMessage(fieldErrors, error.message),
        fieldErrors: fieldErrors,
      );
    }

    if (error.message.trim().isNotEmpty) {
      return ApiErrorUiMessage(message: error.message.trim());
    }

    return ApiErrorUiMessage(message: fallbackMessage);
  }

  static Map<String, String> _extractFieldErrors(Object? details) {
    if (details is! Map) {
      return const {};
    }

    final fieldErrors = <String, String>{};
    for (final entry in details.entries) {
      final key = entry.key?.toString().trim() ?? '';
      final value = entry.value?.toString().trim() ?? '';
      if (key.isEmpty || value.isEmpty) {
        continue;
      }

      fieldErrors[key] = _formatFieldMessage(key, value);
    }

    return fieldErrors;
  }

  static String _validationMessage(
    Map<String, String> fieldErrors,
    String backendMessage,
  ) {
    if (fieldErrors.isEmpty) {
      return 'Please check the highlighted fields and try again.';
    }

    if (fieldErrors.length == 1) {
      return fieldErrors.values.first;
    }

    if (backendMessage.trim().isNotEmpty &&
        backendMessage.toLowerCase() != 'request validation failed') {
      return backendMessage.trim();
    }

    return 'Please check the highlighted fields and try again.';
  }

  static String _formatFieldMessage(String field, String message) {
    final label = _humanizeField(field);
    final normalizedMessage = message.trim();

    if (normalizedMessage.startsWith(label)) {
      return normalizedMessage;
    }

    return '$label $normalizedMessage';
  }

  static String _humanizeField(String field) {
    switch (field) {
      case 'usernameOrEmail':
        return 'Username or email';
      case 'fullName':
        return 'Full name';
      case 'phoneNumber':
        return 'Phone number';
      case 'confirmPassword':
        return 'Confirm password';
      case 'newPassword':
        return 'New password';
      case 'resetToken':
        return 'Reset token';
      case 'challengeToken':
        return 'Challenge token';
      default:
        return field.isEmpty
            ? 'Field'
            : field[0].toUpperCase() + field.substring(1);
    }
  }
}
